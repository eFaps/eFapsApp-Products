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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("c715f832-ac54-46de-bc39-1ca3b704315f")
@EFapsApplication("eFapsApp-Products")
public abstract class BOMCalculator_Base
    extends AbstractCommon
{

    /**
     * Logger for this instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BOMCalculator.class);

    /**
     * Instance of the product.
     */
    private Instance productInstance;

    /**
     * Quantity in Stock.
     */
    private BigDecimal quantityStock;

    /**
     * Quantity required.
     */
    private BigDecimal quantityRequired;

    /**
     * Quantity that is possible to produce or arm using the available stock.
     */
    private BigDecimal quantityOnPaper;

    /**
     * Parameter.
     */
    private Parameter parameter;

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _prodInst instance of the product to be produced
     * @param _quantityRequired quantity requiered
     * @throws EFapsException on error
     */
    public BOMCalculator_Base(final Parameter _parameter,
                              final Instance _prodInst,
                              final BigDecimal _quantityRequired)
        throws EFapsException
    {
        setQuantityRequired(_quantityRequired);
        setProductInstance(_prodInst);
    }

    /**
     * Getter method for the instance variable {@link #parameter}.
     *
     * @return value of instance variable {@link #parameter}
     */
    public Parameter getParameter()
    {
        return this.parameter;
    }

    /**
     * Setter method for instance variable {@link #parameter}.
     *
     * @param _parameter value for instance variable {@link #parameter}
     */
    public void setParameter(final Parameter _parameter)
    {
        this.parameter = _parameter;
    }

    /**
     * Getter method for the instance variable {@link #productInstance}.
     *
     * @return value of instance variable {@link #productInstance}
     */
    public Instance getProductInstance()
    {
        return this.productInstance;
    }

    /**
     * Setter method for instance variable {@link #productInstance}.
     *
     * @param _productInstance value for instance variable
     *            {@link #productInstance}
     */
    public void setProductInstance(final Instance _productInstance)
    {
        this.productInstance = _productInstance;
    }

    /**
     * Getter method for the instance variable {@link #quantityStock}.
     *
     * @return value of instance variable {@link #quantityStock}
     * @throws EFapsException on error
     */
    public BigDecimal getQuantityStock()
        throws EFapsException
    {
        if (this.quantityStock == null) {
            setQuantityStock(getStock(getParameter(), getProductInstance()));
        }
        return this.quantityStock;
    }

    /**
     * Setter method for instance variable {@link #quantityRequired}.
     *
     * @param _quantity value for instance variable {@link #quantityRequired}
     */
    public void setQuantityRequired(final BigDecimal _quantity)
    {
        this.quantityRequired = _quantity == null ? BigDecimal.ZERO : _quantity;
    }

    /**
     * Getter method for the instance variable {@link #quantityRequired}.
     *
     * @return value of instance variable {@link #quantityRequired}
     */
    public BigDecimal getQuantityRequired()
    {
        return this.quantityRequired;
    }

    /**
     * Setter method for instance variable {@link #quantityStock}.
     *
     * @param _quantityStock value for instance variable {@link #quantityStock}
     */
    public void setQuantityStock(final BigDecimal _quantityStock)
    {
        this.quantityStock = _quantityStock;
    }

    /**
     * Getter method for the instance variable {@link #quantityOnPaper}.
     *
     * @return value of instance variable {@link #quantityOnPaper}
     * @throws EFapsException on error
     */
    public BigDecimal getQuantityOnPaper()
        throws EFapsException
    {
        if (this.quantityOnPaper == null) {
            setQuantityOnPaper(getStock4OnPaper(getParameter(), getProductInstance()));
        }
        return this.quantityOnPaper;
    }

    /**
     * Setter method for instance variable {@link #quantityOnPaper}.
     *
     * @param _quantityOnPaper value for instance variable
     *            {@link #quantityOnPaper}
     */
    public void setQuantityOnPaper(final BigDecimal _quantityOnPaper)
    {
        this.quantityOnPaper = _quantityOnPaper;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _prodInst instance of the product to be produced
     * @return possible producable
     * @throws EFapsException on error
     */
    protected BigDecimal getStock(final Parameter _parameter,
                                  final Instance _prodInst)
        throws EFapsException
    {
        BigDecimal stock = BigDecimal.ZERO;
        final QueryBuilder queryBuild = new QueryBuilder(CIProducts.Inventory);
        queryBuild.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInst);

        add2QueryBuilder4Stock(_parameter, queryBuild);
        final MultiPrintQuery multi = queryBuild.getPrint();
        multi.addAttribute(CIProducts.Inventory.Quantity);
        multi.execute();
        if (multi.next()) {
            stock = multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
        }
        return stock;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _queryBldr queryBldr to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder4Stock(final Parameter _parameter,
                                          final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance storGrpInstance = Products.DEFAULTSTORAGEGRP4BOM.get();
        if (storGrpInstance != null && storGrpInstance.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                            storGrpInstance);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.Inventory.Storage, attrQuery);
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _prodInst instance of the product to be produced
     * @return possible producable
     * @throws EFapsException on error
     */
    protected BigDecimal getStock4OnPaper(final Parameter _parameter,
                                          final Instance _prodInst)
        throws EFapsException
    {
        BigDecimal stock = BigDecimal.ZERO.subtract(BigDecimal.ONE);
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductionBOM);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductionBOM.From, _prodInst);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.ProductionBOM.To).instance();
        multi.addSelect(selProdInst);
        multi.addAttribute(CIProducts.ProductionBOM.Quantity, CIProducts.ProductionBOM.UoM);
        multi.execute();
        final Map<Instance, BigDecimal> stocks = new HashMap<>();
        final Map<Instance, BigDecimal> req = new HashMap<>();
        while (multi.next()) {
            final Instance prodInst = multi.getSelect(selProdInst);
            final BigDecimal quantity = multi.<BigDecimal>getAttribute(CIProducts.ProductionBOM.Quantity);
            final UoM uoM = Dimension.getUoM(multi.<Long>getAttribute(CIProducts.ProductionBOM.UoM));

            final BigDecimal reqQuan = quantity.multiply(new BigDecimal(uoM.getNumerator()).setScale(8).divide(
                            new BigDecimal(uoM.getDenominator()), BigDecimal.ROUND_HALF_UP));
            req.put(prodInst, reqQuan);
            final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.Inventory);
            queryBldr2.addWhereAttrEqValue(CIProducts.Inventory.Product, prodInst);
            add2QueryBuilder4OnPaper(_parameter, queryBldr2);
            final MultiPrintQuery multi2 = queryBldr2.getPrint();
            multi2.addAttribute(CIProducts.Inventory.Quantity);
            multi2.execute();

            while (multi2.next()) {
                BigDecimal currStock = BigDecimal.ZERO;
                if (stocks.containsKey(prodInst)) {
                    currStock = stocks.get(prodInst);
                }
                stocks.put(prodInst, currStock.add(multi2.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity)));
            }
        }
        boolean hasStock = true;
        while (hasStock) {
            stock = stock.add(BigDecimal.ONE);
            for (final Entry<Instance, BigDecimal> reqEntry : req.entrySet()) {
                if (stocks.containsKey(reqEntry.getKey())) {
                    final BigDecimal newStock = stocks.get(reqEntry.getKey()).subtract(reqEntry.getValue());
                    if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                        hasStock = false;
                        break;
                    } else {
                        stocks.put(reqEntry.getKey(), newStock);
                    }
                } else {
                    hasStock = false;
                    break;
                }
            }
        }
        return stock;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @param _queryBldr queryBldr to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder4OnPaper(final Parameter _parameter,
                                            final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Instance storGrpInstance = Products.DEFAULTSTORAGEGRP4BOM.get();
        if (storGrpInstance != null && storGrpInstance.isValid()) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StorageGroupAbstract2StorageAbstract);
            attrQueryBldr.addWhereAttrEqValue(CIProducts.StorageGroupAbstract2StorageAbstract.FromAbstractLink,
                            storGrpInstance);
            final AttributeQuery attrQuery = attrQueryBldr
                            .getAttributeQuery(CIProducts.StorageGroupAbstract2StorageAbstract.ToAbstractLink);
            _queryBldr.addWhereAttrInQuery(CIProducts.Inventory.Storage, attrQuery);
        }
    }
}
