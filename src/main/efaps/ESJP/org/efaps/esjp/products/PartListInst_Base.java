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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * 
 */
@EFapsUUID("56c7e8e9-07e2-4ea4-97ef-16e263a68d65")
@EFapsApplication("eFapsApp-Products")
public class PartListInst_Base
{
    /**
     * Instance of the PartLIst represented by this class.
     */
    private final Instance instance;

    /**
     * Quantity this PartList can be armed from the Stock of products.
     */
    private Integer stockQuantity;

    /**
     * @param _instance
     */
    public PartListInst_Base(final Instance _instance)
    {
       this.instance = _instance;
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
    {
        return this.instance;
    }


    public void evalStockQuantity()
        throws EFapsException
    {
        final Map<String, BigDecimal> must = new HashMap<>();
        final Map<String, BigDecimal> stock = new HashMap<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.SalesBOM);
        queryBldr.addWhereAttrEqValue(CIProducts.SalesBOM.From, this.instance.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = new SelectBuilder().linkto(CIProducts.SalesBOM.To).oid();
        multi.addAttribute(CIProducts.SalesBOM.Quantity);
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            BigDecimal quantityTmp = multi.<BigDecimal>getAttribute(CIProducts.SalesBOM.Quantity);
            final String oid = multi.<String>getSelect(sel);
            if (must.containsKey(oid))
            {
                quantityTmp = quantityTmp.add(must.get(oid));
            }
            must.put(oid, quantityTmp);
        }

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.SalesBOM);
        attrQueryBldr.addWhereAttrEqValue(CIProducts.SalesBOM.From, this.instance.getId());
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.SalesBOM.To);

        final QueryBuilder stockQueryBldr = new QueryBuilder(CIProducts.Inventory);
        stockQueryBldr.addWhereAttrInQuery(CIProducts.Inventory.Product, attrQuery);
        final MultiPrintQuery stockMulti = stockQueryBldr.getPrint();
        stockMulti.addAttribute(CIProducts.Inventory.Quantity);
        final SelectBuilder sel2 = new SelectBuilder().linkto(CIProducts.Inventory.Product).oid();
        stockMulti.addSelect(sel2);
        stockMulti.execute();
        while (stockMulti.next()) {
            BigDecimal quantityTmp = stockMulti.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            final String oid = stockMulti.<String>getSelect(sel2);
            if (stock.containsKey(oid))
            {
                quantityTmp = quantityTmp.add(stock.get(oid));
            }
            stock.put(oid, quantityTmp);
        }
        boolean check = true;
        this.stockQuantity = 0;
        if (!must.isEmpty()) {
            while (check) {
                for (final Entry<String, BigDecimal> entry : must.entrySet()) {
                    if (stock.containsKey(entry.getKey())
                                    && stock.get(entry.getKey()).subtract(entry.getValue()).signum() > -1) {
                          stock.put(entry.getKey(), stock.get(entry.getKey()).subtract(entry.getValue()));
                    } else {
                        check = false;
                    }
                }
                if (check) {
                    this.stockQuantity++;
                }
            }
        }
    }

    /**
     * Getter method for the instance variable {@link #stockQuantity}.
     *
     * @return value of instance variable {@link #stockQuantity}
     * @throws EFapsException on error
     */
    public Integer getStockQuantity()
        throws EFapsException
    {
        if (this.stockQuantity == null) {
            evalStockQuantity();
        }
        return this.stockQuantity;
    }


    /**
     * Setter method for instance variable {@link #stockQuantity}.
     *
     * @param _stockQuantity value for instance variable {@link #stockQuantity}
     */

    public void setStockQuantity(final Integer _stockQuantity)
    {
        this.stockQuantity = _stockQuantity;
    }


}
