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
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.event.Parameter;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BOMCalculator_Base
{
    protected static final Logger LOG = LoggerFactory.getLogger(BOMCalculator.class);

    private BigDecimal quantityStock;

    private BigDecimal quantityRequired;

    private String oid;

    private String typeName;

    private int factory = 0;

    public BOMCalculator_Base(final Parameter _parameter,
                              final Instance _prodInst,
                              final BigDecimal _quantityRequired)
        throws EFapsException
    {
        setQuantityStock(getStock2Product(_parameter, _prodInst));
        setQuantityRequired(_quantityRequired);
        setOid(_prodInst.getOid());
        setTypeName(_prodInst.getType().getName());

        setFactory(_quantityRequired, this.quantityStock);
    }

    protected BigDecimal getStock2Product(final Parameter _parameter,
                                          final Instance _prodInst)
        throws EFapsException
    {
        BigDecimal stock = BigDecimal.ZERO;
        QueryBuilder queryBuild = new QueryBuilder(CIProducts.Inventory);
        queryBuild.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInst);

        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        final Instance storGrpInstance = config.getLink("org.efaps.sales.StorageGroup4ProductBOM");
        if (storGrpInstance != null && storGrpInstance.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                            storGrpInstance);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);

            queryBuild.addWhereAttrInQuery(CIProducts.Inventory.Storage, attrQuery);

            add2QueryBuilder2Storage(_parameter, queryBuild);
            final MultiPrintQuery multi = queryBuild.getPrint();
            multi.addAttribute(CIProducts.Inventory.Quantity);
            multi.execute();
            if (multi.next()) {
                stock = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            }
        } else {
            BOMCalculator_Base.LOG.warn("It's required a system configuration for Storage Group");
        }
        return stock;
    }

    public void add2QueryBuilder2Storage(final Parameter _parameter,
                                         final QueryBuilder query)
    {
        // to implement
    }

    public void setFactory(final BigDecimal _quantityRequired,
                           final BigDecimal _quantityStock)
    {
        if (_quantityStock.compareTo(BigDecimal.ZERO) != 0) {
            this.factory = ((_quantityStock).divide(_quantityRequired,BigDecimal.ROUND_HALF_DOWN)).intValue();
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

    public void setQuantityStock(final BigDecimal _quantity)
        throws EFapsException
    {
        this.quantityStock = _quantity != null && _quantity.compareTo(BigDecimal.ZERO)==1 ? _quantity : BigDecimal.ZERO;
    }

    public Instance getProductBOMInstance()
    {
        return Instance.get(getOid());
    }

    public BigDecimal getQuantityRequired()
    {
        return quantityRequired;
    }

    public void setQuantityRequired(final BigDecimal _quantity)
        throws EFapsException
    {
        this.quantityRequired = _quantity != null && _quantity.compareTo(BigDecimal.ZERO)==1 ? _quantity : BigDecimal.ZERO;
    }

    public String getTypeName()
    {
        return typeName;
    }

    public void setTypeName(final String _typeName)
    {
        this.typeName = _typeName;
    }

}
