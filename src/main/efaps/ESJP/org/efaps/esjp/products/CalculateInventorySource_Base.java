/*
 * Copyright 2003 - 2010 The eFaps Team
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.EFapsMapDataSource;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("3ec02cd1-7e56-4a6c-b45e-8b09dfb2d4a9")
@EFapsApplication("eFapsApp-Products")
public abstract class CalculateInventorySource_Base
    extends EFapsMapDataSource
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final JasperReport _jasperReport,
                     final Parameter _parameter,
                     final JRDataSource _parentSource,
                     final Map<String, Object> _jrParameters)
        throws EFapsException
    {
        final String storageOid = _parameter.getParameterValue("storage");
        final String dateStr = _parameter.getParameterValue("date");
        // get a new DateTime from the ISO Date String fomr the Parameters
        final DateTime date = new DateTime(dateStr);
        _jrParameters.put("Storage", _parameter.getParameterValue("storageAutoComplete"));
        _jrParameters.put("Date", date.toDate());
        final Instance storageInst = Instance.get(storageOid);

        final Map<String, BigDecimal> actual = new HashMap<>();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storageInst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        final SelectBuilder sel = new SelectBuilder().linkto(CIProducts.Inventory.Product).oid();
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            final String oid = multi.<String>getSelect(sel);
            final BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            actual.put(oid, quantity);
        }

        final QueryBuilder transQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
        transQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Storage, storageInst.getId());
        transQueryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, date.minusMinutes(1));
        transQueryBldr.getQuery();

        final MultiPrintQuery transMulti = transQueryBldr.getPrint();
        transMulti.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                                CIProducts.TransactionInOutAbstract.UoM);
        final SelectBuilder transSel = new SelectBuilder().linkto(CIProducts.TransactionInOutAbstract.Product).oid();
        transMulti.addSelect(transSel);
        transMulti.execute();
        while (transMulti.next()) {
            final String oid = transMulti.<String>getSelect(transSel);
            BigDecimal quantity = transMulti.<BigDecimal>getAttribute(CIProducts.TransactionInbound.Quantity);
            final Long uoMId = transMulti.<Long>getAttribute(CIProducts.TransactionInbound.UoM);
            final UoM uoM = Dimension.getUoM(uoMId);
            quantity = quantity.multiply(new BigDecimal(uoM.getNumerator())
                                            .divide(new BigDecimal(uoM.getDenominator())));
            final Instance inst = transMulti.getCurrentInstance();
            if (inst.getType().isKindOf(CIProducts.TransactionInbound.getType())) {
                quantity = quantity.negate();
            }
            if (actual.containsKey(oid)) {
                quantity = actual.get(oid).add(quantity);
            }
            actual.put(oid, quantity);
        }
        final List<Instance> instances = new ArrayList<>();
        for (final Entry<String, BigDecimal> entry : actual.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                instances.add(Instance.get(entry.getKey()));
            }
        }
        final MultiPrintQuery multiRes = new MultiPrintQuery(instances);
        multiRes.addAttribute(CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description,
                        CIProducts.ProductAbstract.Dimension);
        multiRes.execute();
        while (multiRes.next()) {
            final Map<String, Object> value = new HashMap<>();
            getValues().add(value);
            value.put("Name", multiRes.getAttribute(CIProducts.ProductAbstract.Name));
            value.put("Description", multiRes.getAttribute(CIProducts.ProductAbstract.Description));
            final UoM uom = Dimension.get(multiRes.<Long>getAttribute(CIProducts.ProductAbstract.Dimension)).getBaseUoM();
            value.put("UoM", uom.getName());
            value.put("Quantity", actual.get(multiRes.getCurrentInstance().getOid()));
            value.put("ProductOid", multiRes.getCurrentInstance().getOid());
        }
        Collections.sort(getValues(), new Comparator<Map<String, Object>>(){
            @Override
            public int compare(final Map<String, Object> _map1,
                               final Map<String, Object> _map2)
            {
                final String name1 = (String) _map1.get("Name");
                final String name2 = (String) _map2.get("Name");
                return name1.compareTo(name2);
            }
        });
    }
}
