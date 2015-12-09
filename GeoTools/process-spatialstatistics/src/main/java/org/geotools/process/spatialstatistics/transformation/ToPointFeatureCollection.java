/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.transformation;

import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.process.spatialstatistics.core.FeatureTypes;
import org.geotools.process.spatialstatistics.core.FeatureTypes.SimpleShapeType;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Feature To Point SimpleFeatureCollection Implementation
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class ToPointFeatureCollection extends GXTSimpleFeatureCollection {
    protected static final Logger LOGGER = Logging.getLogger(ToPointFeatureCollection.class);

    private boolean useInside;

    private SimpleFeatureType schema;

    public ToPointFeatureCollection(SimpleFeatureCollection delegate, boolean useInside) {
        super(delegate);

        this.useInside = useInside;

        String typeName = delegate.getSchema().getTypeName();
        this.schema = FeatureTypes.build(delegate.getSchema(), typeName, Point.class);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new ToPointFeatureIterator(delegate.features(), getSchema(), useInside);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    static class ToPointFeatureIterator implements SimpleFeatureIterator {
        private SimpleFeatureIterator delegate;

        private boolean useInside = false;

        private SimpleFeatureBuilder builder;

        private SimpleShapeType shapeType;

        public ToPointFeatureIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema,
                boolean useInside) {
            this.delegate = delegate;

            this.useInside = useInside;
            this.builder = new SimpleFeatureBuilder(schema);
            this.shapeType = FeatureTypes.getSimpleShapeType(schema);
        }

        public void close() {
            delegate.close();
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public SimpleFeature next() throws NoSuchElementException {
            SimpleFeature sourceFeature = delegate.next();
            SimpleFeature nextFeature = builder.buildFeature(sourceFeature.getID());

            // transfer attributes
            transferAttribute(sourceFeature, nextFeature);

            // centroid or interior point
            Geometry geometry = (Geometry) sourceFeature.getDefaultGeometry();
            Point center = geometry.getCentroid();
            if (useInside && shapeType == SimpleShapeType.POLYGON && !geometry.contains(center)) {
                center = geometry.getInteriorPoint();
            }
            nextFeature.setDefaultGeometry(center);

            return nextFeature;
        }
    }
}