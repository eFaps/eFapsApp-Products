/*
 * Copyright 2003 - 2012 The eFaps Team

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
 * Revision:        $Rev: 8342 $
 * Last Changed:    $Date: 2012-12-11 09:42:17 -0500 (Tue, 11 Dec 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;

import org.efaps.admin.event.Parameter;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.util.EFapsException;

public abstract class BOMCalculator_Base
{

    private BigDecimal quantityStock;

    private BigDecimal quantityRequired;

    private String oid;

    private final String typeName;

    private int factory = 0;

    public BOMCalculator_Base(final Parameter _parameter,
                              final Instance _prodInst,
                              final BigDecimal _quantityRequired)
        throws EFapsException
    {
        this.quantityStock = getStcok2Product(_parameter, _prodInst);
        this.quantityRequired = _quantityRequired;
        this.oid = _prodInst.getOid();
        this.typeName = _prodInst.getType().getName();

        setFactory(_quantityRequired, this.quantityStock);
    }

    protected BigDecimal getStcok2Product(final Parameter _parameter,
                                          final Instance _prodInst)
        throws EFapsException
    {
        BigDecimal stock = BigDecimal.ZERO;
        QueryBuilder queryBuild = new QueryBuilder(CIProducts.Inventory);
        queryBuild.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInst);
        add2QueryBuilder2Storage(_parameter, queryBuild);
        final MultiPrintQuery multi = queryBuild.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        multi.execute();
        if (multi.next()) {
            stock = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
        }
        return stock;
    }

    public void add2QueryBuilder2Storage(final Parameter _parameter,
                                         final QueryBuilder query)
    {
        // to implement
    }

    public BigDecimal parse(final String _value)
        throws EFapsException
    {
        final BigDecimal ret;
        try {
            ret = (BigDecimal) getFormatter().parse(_value);
        } catch (final ParseException e) {
            throw new EFapsException(BOMCalculator.class, "ParseException", e);
        }
        return ret;
    }

    protected DecimalFormat getFormatter()
        throws EFapsException
    {
        return NumberFormatter.get().getFormatter();
    }

    public void setFactory(final BigDecimal _quantityRequired,
                           final BigDecimal _quantityStock)
    {
        if (_quantityStock.compareTo(BigDecimal.ZERO) != 0) {
            this.factory = ((_quantityStock).divide(_quantityRequired, BigDecimal.ROUND_HALF_UP)).intValue();
        }

    }

    public int getFactory()
    {
        return factory;
    }

    public String getOid()
    {
        return oid;
    }

    public void setOid(final String oid)
    {
        this.oid = oid;
    }

    public BigDecimal getQuantityStock()
    {
        return quantityStock;
    }

    public void setQuantityStock(final String _quantity)
        throws EFapsException
    {
        this.quantityStock = _quantity != null && _quantity.length() > 0 ? parse(_quantity) : BigDecimal.ZERO;
    }

    public Instance getProductBOMInstance()
    {
        return Instance.get(getOid());
    }

    public BigDecimal getQuantityRequired()
    {
        return quantityRequired;
    }

    public void setQuantityRequired(final String _quantity)
        throws EFapsException
    {
        this.quantityRequired = _quantity != null && _quantity.length() > 0 ? parse(_quantity) : BigDecimal.ZERO;
    }

    public String getTypeName()
    {
        return typeName;
    }

}
