/*
 * Copyright 2003 - 2016 The eFaps Team
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
 */
package org.efaps.esjp.products;

import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.products.util.Products.CostingState;
import org.efaps.util.EFapsException;

/**
 * The Class Costing_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("c6703bbf-4b29-4946-b77c-0a99ca9907bf")
@EFapsApplication("eFapsApp-Products")
public abstract class Costing_Base
    extends AbstractCommon
{

    /**
     * Sets the fixed.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return setFixed(final Parameter _parameter)
        throws EFapsException
    {
        final Instance inst = _parameter.getInstance();

        final PrintQuery print = new PrintQuery(inst);
        final SelectBuilder prodInstSel = SelectBuilder.get().linkto(CIProducts.CostingAbstract.TransactionAbstractLink)
                        .linkto(CIProducts.TransactionAbstract.Product).instance();
        print.addSelect(prodInstSel);
        print.addAttribute(CIProducts.CostingAbstract.CurrencyLink);
        print.execute();
        final Instance prodInst = print.getSelect(prodInstSel);
        final Long currencyId = print.getAttribute(CIProducts.CostingAbstract.CurrencyLink);

        final QueryBuilder cAttrQueryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
        cAttrQueryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, prodInst);

        final QueryBuilder cQeryBldr = new QueryBuilder(inst.getType());
        cQeryBldr.addWhereAttrEqValue(CIProducts.CostingAbstract.CurrencyLink, currencyId);
        cQeryBldr.addWhereAttrInQuery(CIProducts.CostingAbstract.TransactionAbstractLink, cAttrQueryBldr
                        .getAttributeQuery(CIProducts.TransactionAbstract.ID));

        final MultiPrintQuery cmulti = cQeryBldr.getPrint();
        final SelectBuilder transInstSel = SelectBuilder.get().linkto(
                        CIProducts.CostingAbstract.TransactionAbstractLink).instance();
        cmulti.addSelect(transInstSel);
        cmulti.executeWithoutAccessCheck();
        final Map<Instance, Instance> map = new HashMap<>();
        while (cmulti.next()) {
            final Instance transInst = cmulti.getSelect(transInstSel);
            map.put(transInst, cmulti.getCurrentInstance());
        }

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, prodInst);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionAbstract.Date);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionAbstract.Position);
        queryBldr.addOrderByAttributeAsc(CIProducts.TransactionAbstract.Modified);
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        CostingState state = CostingState.INACTIVE;
        while (query.next()) {
            final Instance transInst = query.getCurrentValue();
            if (map.containsKey(transInst)) {
                final Instance costingInst = map.get(transInst);

                final Update update = new Update(costingInst);

                if (costingInst.equals(inst)) {
                    state = CostingState.FIXED;
                    update.add(CIProducts.CostingAbstract.Result, _parameter.getParameterValue(
                                    CIFormProducts.Products_ProductCostingForm.result.name));
                    update.add(CIProducts.CostingAbstract.UpToDate, false);
                }
                update.add(CIProducts.CostingAbstract.State, state);
                update.execute();
                if (costingInst.equals(inst)) {
                    state = CostingState.ACTIVE;
                }
            }
        }
        return new Return();
    }
}
