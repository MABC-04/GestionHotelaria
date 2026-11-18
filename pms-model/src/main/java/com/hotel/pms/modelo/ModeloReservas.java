package com.hotel.pms.modelo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Fachada de acceso al modelo {@code reservas.ecore}.
 *
 * <p>El modelo se carga como <em>EMF dinámico</em>: no hay clases Java
 * generadas en el Sprint 0, por lo que las instancias son {@link EObject}
 * cuyo {@code eClass()} describe su estructura. La generación de entidades JPA
 * es una tarea posterior del pipeline (G1 del §5.3).</p>
 */
public final class ModeloReservas {

    public static final String NS_URI = "http://www.hotel.com/pms/reservas";
    private static final String RUTA_METAMODELO = "/model/reservas.ecore";

    private final EPackage paquete;
    private final Resource resource;

    private ModeloReservas(EPackage paquete, Resource resource) {
        this.paquete = paquete;
        this.resource = resource;
    }

    /** Carga el metamodelo y deja un contenedor de instancias vacío. */
    public static ModeloReservas vacio() {
        ResourceSet rs = nuevoResourceSet();
        EPackage paquete = cargarMetamodelo(rs);
        Resource instancias = rs.createResource(URI.createURI("memoria:/instancias.xmi"));
        return new ModeloReservas(paquete, instancias);
    }

    /** Carga el metamodelo y un archivo de instancias {@code .xmi}. */
    public static ModeloReservas cargar(Path xmi) {
        ResourceSet rs = nuevoResourceSet();
        EPackage paquete = cargarMetamodelo(rs);
        Resource instancias = rs.getResource(URI.createFileURI(xmi.toAbsolutePath().toString()), true);
        exigirSinErrores(instancias, xmi.toString());
        return new ModeloReservas(paquete, instancias);
    }

    /** Carga el metamodelo y un fixture empaquetado en el classpath. */
    public static ModeloReservas cargarDeClasspath(String ruta) {
        URL url = ModeloReservas.class.getResource(ruta);
        if (url == null) {
            throw new IllegalArgumentException("No existe el recurso en el classpath: " + ruta);
        }
        ResourceSet rs = nuevoResourceSet();
        EPackage paquete = cargarMetamodelo(rs);
        Resource instancias = rs.getResource(URI.createURI(url.toString()), true);
        exigirSinErrores(instancias, ruta);
        return new ModeloReservas(paquete, instancias);
    }

    private static ResourceSet nuevoResourceSet() {
        ResourceSetImpl rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        return rs;
    }

    private static EPackage cargarMetamodelo(ResourceSet rs) {
        URL url = ModeloReservas.class.getResource(RUTA_METAMODELO);
        if (url == null) {
            throw new IllegalStateException("No se encontró " + RUTA_METAMODELO + " en el classpath");
        }
        Resource recurso = rs.getResource(URI.createURI(url.toString()), true);
        exigirSinErrores(recurso, RUTA_METAMODELO);
        EPackage paquete = (EPackage) recurso.getContents().get(0);
        // Fábrica propia para (de)serializar UUID, fechas, horas y decimales.
        paquete.setEFactoryInstance(new FactoryReservas());
        rs.getPackageRegistry().put(NS_URI, paquete);
        return paquete;
    }

    private static void exigirSinErrores(Resource recurso, String origen) {
        EList<Resource.Diagnostic> errores = recurso.getErrors();
        if (!errores.isEmpty()) {
            StringBuilder sb = new StringBuilder("Errores al cargar " + origen + ":\n");
            for (Resource.Diagnostic d : errores) {
                sb.append("  - [").append(d.getLine()).append(':').append(d.getColumn()).append("] ")
                        .append(d.getMessage()).append('\n');
            }
            throw new IllegalStateException(sb.toString());
        }
    }

    // ---------------------------------------------------------------- metamodelo

    public EPackage paquete() {
        return paquete;
    }

    public EClass clase(String nombre) {
        EClassifier c = paquete.getEClassifier(nombre);
        if (!(c instanceof EClass eClass)) {
            throw new IllegalArgumentException("Clase inexistente en el metamodelo: " + nombre);
        }
        return eClass;
    }

    public EEnum enumeracion(String nombre) {
        EClassifier c = paquete.getEClassifier(nombre);
        if (!(c instanceof EEnum eEnum)) {
            throw new IllegalArgumentException("Enumeración inexistente en el metamodelo: " + nombre);
        }
        return eEnum;
    }

    public EEnumLiteral literal(String enumeracion, String literal) {
        EEnumLiteral l = enumeracion(enumeracion).getEEnumLiteral(literal);
        if (l == null) {
            throw new IllegalArgumentException("Literal inexistente: " + enumeracion + "::" + literal);
        }
        return l;
    }

    public EAttribute atributo(String clase, String atributo) {
        EStructuralFeature f = clase(clase).getEStructuralFeature(atributo);
        if (!(f instanceof EAttribute a)) {
            throw new IllegalArgumentException("Atributo inexistente: " + clase + "." + atributo);
        }
        return a;
    }

    // ---------------------------------------------------------------- instancias

    /** Objetos raíz del recurso de instancias. */
    public List<EObject> objetos() {
        return new ArrayList<>(resource.getContents());
    }

    public List<EObject> de(String clase) {
        EClass eClass = clase(clase);
        List<EObject> resultado = new ArrayList<>();
        for (java.util.Iterator<EObject> it = EcoreUtil.getAllContents(resource, true); it.hasNext(); ) {
            EObject o = it.next();
            if (eClass.isInstance(o)) {
                resultado.add(o);
            }
        }
        return resultado;
    }

    public Optional<EObject> primero(String clase) {
        return de(clase).stream().findFirst();
    }

    public EObject nuevo(String clase) {
        return EcoreUtil.create(clase(clase));
    }

    /** Añade una instancia como raíz del recurso. */
    public EObject agregar(EObject objeto) {
        resource.getContents().add(objeto);
        return objeto;
    }

    public void guardar(Path destino) {
        resource.setURI(URI.createFileURI(destino.toAbsolutePath().toString()));
        try {
            resource.save(null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Diagnóstico estructural del metamodelo (sin instancias). */
    public static List<Diagnostic> diagnosticar(EPackage paquete) {
        List<Diagnostic> out = new ArrayList<>();
        for (EClassifier c : paquete.getEClassifiers()) {
            if (c instanceof EClass eClass) {
                for (EStructuralFeature f : eClass.getEStructuralFeatures()) {
                    if (f.getEType() == null) {
                        out.add(crearError("Feature sin tipo: " + eClass.getName() + "." + f.getName()));
                    }
                    if (f.getEType() != null && f.getEType().eIsProxy()) {
                        out.add(crearError("Tipo no resuelto en " + eClass.getName() + "." + f.getName()));
                    }
                }
            }
        }
        return out;
    }

    private static Diagnostic crearError(String mensaje) {
        return new org.eclipse.emf.common.util.BasicDiagnostic(
                Diagnostic.ERROR, "pms-model", 1, mensaje, null);
    }
}
