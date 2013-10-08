/*
 * Copyright 2003 - 2013 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.products.util.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class ImportProducts
{

    private static Logger LOG = LoggerFactory.getLogger(ImportProducts.class);

    public Return importProducts(final Parameter _parameter)
    {
        final String filename = _parameter.getParameterValue("valueField");
        final File file = new File(filename);
        if (checkData(file)) {
            importData(file, CIProducts.ProductMaterial);
        }
        return new Return();
    }


    /**
     * @param _file
     */
    private void importData(final File _file, final CIType _ciType)
    {
        try {
            final CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(_file), "UTF-8"));
            final List<String[]> entries = reader.readAll();
            reader.close();
            entries.remove(0);
            for (final String[] row : entries) {
                final String productDesc = row[0];
                final Insert insert =new Insert(_ciType);
                insert.add(CIProducts.ProductAbstract.Dimension, 1);
                insert.add(CIProducts.ProductAbstract.Description, productDesc);
                insert.add(CIProducts.ProductAbstract.Name, productDesc);
                insert.add(CIProducts.ProductAbstract.SalesUnit, 1);
                insert.add(CIProducts.ProductAbstract.TaxCategory, 1);
                insert.executeWithoutTrigger();

                final Instance prodInst = insert.getInstance();
                final String classificationName = row[1];
                final Classification clazz = Classification.get(classificationName);
                Classification parent = clazz.getParentClassification();
                final List<Classification>clazzes = new ArrayList<Classification>();
                while (parent != null) {
                    clazzes.add(parent);
                    parent = parent.getParentClassification();
                }
                Collections.reverse(clazzes);
                clazzes.add(clazz);
                for (final Classification classification : clazzes) {
                    final Insert relInsert = new Insert(classification.getClassifyRelationType());
                    relInsert.add(classification.getRelLinkAttributeName(), prodInst);
                    relInsert.add(classification.getRelTypeAttributeName(),  classification.getId());
                    relInsert.executeWithoutTrigger();

                    final Insert classInsert = new Insert(classification);
                    classInsert.add(classification.getLinkAttributeName() ,prodInst);
                    classInsert.executeWithoutTrigger();
                }
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final CacheReloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * @param _file
     */
    private boolean checkData(final File _file)
    {
        boolean ret = false;
        try {
            final CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(_file), "UTF-8"));
            final List<String[]> entries = reader.readAll();
            reader.close();
            entries.remove(0);
            int i = 2;
            boolean test = true;
            for (final String[] row : entries) {
                final String classificationName = row[1];
                final Classification clazz = Classification.get(classificationName);
                if (clazz == null) {
                    ImportProducts.LOG.error("Row: {}, Could not find Classification: '{}'", i, classificationName);
                    test = false;
                }
                final String dimensionName = row[2];
                final Dimension dimension = Dimension.get(dimensionName);
                if (dimension == null) {
                    ImportProducts.LOG.error("Row: {}, Could not find Dimension: '{}'", i, dimensionName);
                    test = false;
                }
                i++;
            }
            ret = test;
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final CacheReloadException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }
}
