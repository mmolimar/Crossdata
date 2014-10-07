//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.10.07 at 01:53:35 PM CEST 
//


package com.stratio.meta2.common.api.datastore;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import com.stratio.meta2.common.api.PropertiesType;
import com.stratio.meta2.common.api.PropertyType;

/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class DataStoreFactory {

    private final static QName _DataStore_QNAME = new QName("", "DataStore");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: generated
     * 
     */
    public DataStoreFactory() {
    }

    /**
     * Create an instance of {@link DataStoreType }
     * 
     */
    public DataStoreType createDataStoreType() {
        return new DataStoreType();
    }

    /**
     * Create an instance of {@link com.stratio.meta2.common.api.PropertiesType }
     * 
     */
    public PropertiesType createPropertiesType() {
        return new PropertiesType();
    }

    /**
     * Create an instance of {@link com.stratio.meta2.common.api.PropertyType }
     * 
     */
    public PropertyType createPropertyType() {
        return new PropertyType();
    }

    /**
     * Create an instance of {@link BehaviorsType }
     * 
     */
    public BehaviorsType createBehaviorsType() {
        return new BehaviorsType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DataStoreType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "DataStore")
    public JAXBElement<DataStoreType> createDataStore(DataStoreType value) {
        return new JAXBElement<DataStoreType>(_DataStore_QNAME, DataStoreType.class, null, value);
    }

}
