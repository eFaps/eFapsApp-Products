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

package org.efaps.esjp.products;

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.Naming;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("7c76bfdd-d827-4398-a843-b72299d621f0")
@EFapsRevision("$Rev$")
public abstract class Batch_Base
{

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _prodInst instance of the product that is the base for the Batch
     * @throws EFapsException on error
     * @return new Name
     */
    public String getNewName(final Parameter _parameter,
                             final Instance _prodInst)
        throws EFapsException
    {
        return new Naming().fromNumberGenerator(_parameter, _prodInst, _prodInst.getType().getName() + "4Batch");
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _prodInst instance of the product that is the base for the Batch
     * @throws EFapsException on error
     * @return new Name
     */
    public List<Instance> getExistingBatch4ProductInst(final Parameter _parameter,
                                                       final Instance _prodInst)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductBatch);

        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIProducts.StockProductAbstract2Batch);
        relAttrQueryBldr.addWhereAttrEqValue(CIProducts.StockProductAbstract2Batch.FromLink, _prodInst);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductBatch.ID,
                        relAttrQueryBldr.getAttributeQuery(CIProducts.StockProductAbstract2Batch.ToLink));

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryIndividual);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductBatch.ID,
                        attrQueryBldr.getAttributeQuery(CIProducts.InventoryIndividual.Product));
        return queryBldr.getQuery().executeWithoutAccessCheck();
    }
}
