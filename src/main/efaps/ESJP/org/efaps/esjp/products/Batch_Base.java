/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.Naming;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("7c76bfdd-d827-4398-a843-b72299d621f0")
@EFapsApplication("eFapsApp-Products")
public abstract class Batch_Base
{

    /** The sessionkey. */
    protected static String SESSIONKEY = Batch.class.getName() + ".SessionKey";

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

        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract2Batch);
        relAttrQueryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract2Batch.FromLink, _prodInst);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductBatch.ID,
                        relAttrQueryBldr.getAttributeQuery(CIProducts.StoreableProductAbstract2Batch.ToLink));

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryIndividual);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductBatch.ID,
                        attrQueryBldr.getAttributeQuery(CIProducts.InventoryIndividual.Product));
        return queryBldr.getQuery().executeWithoutAccessCheck();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @throws EFapsException on error
     * @return empty Return
     */
    public Return combine(final Parameter _parameter)
        throws EFapsException
    {
        if (Context.getThreadContext().containsSessionAttribute(SESSIONKEY)) {
            @SuppressWarnings("unchecked")
            final List<Instance> insts = (List<Instance>) Context.getThreadContext().getSessionAttribute(SESSIONKEY);
            if (insts != null && insts.size() == 2) {
                final PrintQuery print = new PrintQuery(insts.get(0));
                print.addAttribute(CIProducts.InventoryIndividual.Quantity, CIProducts.InventoryIndividual.Storage,
                                CIProducts.InventoryIndividual.UoM, CIProducts.InventoryIndividual.Product);
                print.executeWithoutAccessCheck();

                final Insert insert = new Insert(CIProducts.TransactionIndividualOutbound);
                insert.add(CIProducts.TransactionIndividualOutbound.Date, new DateTime());
                insert.add(CIProducts.TransactionIndividualOutbound.Quantity,
                                print.<BigDecimal>getAttribute(CIProducts.InventoryIndividual.Quantity));
                insert.add(CIProducts.TransactionIndividualOutbound.Storage,
                                print.<Long>getAttribute(CIProducts.InventoryIndividual.Storage));
                insert.add(CIProducts.TransactionIndividualOutbound.UoM,
                                print.<Object>getAttribute(CIProducts.InventoryIndividual.UoM));
                insert.add(CIProducts.TransactionIndividualOutbound.Product,
                                print.<Long>getAttribute(CIProducts.InventoryIndividual.Product));
                insert.execute();

                final PrintQuery print2 = new PrintQuery(insts.get(1));
                print2.addAttribute(CIProducts.InventoryIndividual.Product, CIProducts.InventoryIndividual.Storage);
                print2.executeWithoutAccessCheck();

                final Insert insert2 = new Insert(CIProducts.TransactionIndividualInbound);
                insert2.add(CIProducts.TransactionIndividualOutbound.Date, new DateTime());
                insert2.add(CIProducts.TransactionIndividualInbound.Quantity,
                                print.<BigDecimal>getAttribute(CIProducts.InventoryIndividual.Quantity));
                insert2.add(CIProducts.TransactionIndividualInbound.Storage,
                                print2.<Long>getAttribute(CIProducts.InventoryIndividual.Storage));
                insert2.add(CIProducts.TransactionIndividualInbound.UoM,
                                print.<Object>getAttribute(CIProducts.InventoryIndividual.UoM));
                insert2.add(CIProducts.TransactionIndividualOutbound.Product,
                                print2.<Long>getAttribute(CIProducts.InventoryIndividual.Product));
                insert2.execute();
            }
            Context.getThreadContext().removeSessionAttribute(SESSIONKEY);
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @throws EFapsException on error
     * @return Return containing snipplet
     */
    public Return getMessage4CombineFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        StringBuilder html = new StringBuilder();
        final String[] oids = _parameter.getParameterValues("selectedRow");
        final List<Instance> insts = new ArrayList<>();
        for (final String oid : oids) {
            final Instance inst = Instance.get(oid);
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.InventoryIndividual.Product)
                            .instance();
            final SelectBuilder selProdName = SelectBuilder.get().linkto(CIProducts.InventoryIndividual.Product)
                            .attribute(CIProducts.ProductAbstract.Name);
            print.addSelect(selProdInst, selProdName);
            print.executeWithoutAccessCheck();
            final Instance prodInst = print.getSelect(selProdInst);
            if (prodInst.getType().isKindOf(CIProducts.ProductBatch.getType())) {
                final String prodName = print.getSelect(selProdName);
                html.append(prodName).append("</br>");
                insts.add(inst);
            } else {
                html = new StringBuilder();
                insts.clear();
                html.append(DBProperties.getProperty(Batch.class.getName() + ".cannotCombine"));
                break;
            }
        }
        //TODO check almacen, equal product
        if (insts.isEmpty()) {
            Context.getThreadContext().removeSessionAttribute(SESSIONKEY);
        } else {
            Context.getThreadContext().setSessionAttribute(SESSIONKEY, insts);
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }
}
