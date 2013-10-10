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
 * Revision:        $Rev: 10283 $
 * Last Changed:    $Date: 2013-09-24 14:46:54 -0500 (mar, 24 sep 2013) $
 * Last Changed By: $Author: jorge.cueva@moxter.net $
 */


package org.efaps.esjp.products.util.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.List;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: ImportDetails.java 10283 2013-09-24 19:46:54Z jorge.cueva@moxter.net $
 */
public class ImportDetails
{
    private static Logger LOG = LoggerFactory.getLogger(ImportDetails.class);

    public Return importProducts(final Parameter _parameter) {
        final String filename = _parameter.getParameterValue("valueField");
        final File file = new File(filename);
        try {
            insertNewProducts(file, CIProducts.ProductMaterial.getType());
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new Return();
    }

    protected void insertNewProducts(final File _file,
                                     final Type _type)
        throws IOException, EFapsException
    {
        final CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(_file), "UTF-8"));
        final List<String[]> entries = reader.readAll();
        reader.close();
        entries.remove(0);
        int cont = 1;
        for (final String[] row : entries) {
            final String prodDesc = row[0].trim();
            final Classification classification = Classification.get(row[1]);
            if (classification != null) {
                String name = buildName4Product(classification);
                if (name == null) {
                    final Formatter formatter = new Formatter();
                    name = formatter.format("%1$-6s", cont).toString();
                    formatter.close();
                    name = name.replace("\\s", "0");
                }

                final Dimension dim = Dimension.get(row[2]);

                if (dim != null) {
                    final QueryBuilder queryBldr = new QueryBuilder(_type);
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Description, prodDesc);
                    queryBldr.addWhereClassification(classification);
                    final InstanceQuery query = queryBldr.getQuery();
                    query.executeWithoutAccessCheck();

                    if (!query.next()) {
                        final Insert insert = new Insert(_type);
                        insert.add(CIProducts.ProductAbstract.Name, name);
                        insert.add(CIProducts.ProductAbstract.Description, prodDesc);
                        insert.add(CIProducts.ProductAbstract.Active, true);
                        insert.add(CIProducts.ProductAbstract.Dimension, dim.getId());
                        insert.add(CIProducts.ProductAbstract.TaxCategory, true);
                        insert.executeWithoutAccessCheck();

                        if (classification != null) {
                            insertClassification(classification, insert.getInstance());
                        }
                    } else {
                        ImportDetails.LOG.error("The description '{}' with classification '{}' already exist",
                                        row[0], classification);
                    }
                } else {
                    ImportDetails.LOG.error("The dimension '{}' is incorrect or not exist", row[2]);
                }
            } else {
                ImportDetails.LOG.error("The classification '{}' is incorrect or not exist", row[1]);
            }
            cont++;
        }
    }

    protected String buildName4Product(final Classification _classification)
        throws EFapsException
    {
        return null;
    }

    protected void insertClassification(final Classification _classification,
                                        final Instance _prodInstance)
        throws EFapsException
    {
        final Insert relInsert = new Insert(_classification.getClassifyRelationType());
        relInsert.add(_classification.getRelLinkAttributeName(), _prodInstance);
        relInsert.add(_classification.getRelTypeAttributeName(), _classification.getId());
        relInsert.executeWithoutAccessCheck();

        final Insert classInsert = new Insert(_classification);
        classInsert.add(_classification.getLinkAttributeName(), _prodInstance);
        classInsert.executeWithoutAccessCheck();

        if (!_classification.isRoot()) {
            ImportDetails.LOG.info("Child Classification: '{}'", _classification);
            insertClassification(_classification.getParentClassification(), _prodInstance);
        } else {
            ImportDetails.LOG.info("Root Classification: '{}'", _classification);
        }
    }


}
