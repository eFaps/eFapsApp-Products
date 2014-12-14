/*
 * Copyright 2003 - 2014 The eFaps Team
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f2ec6675-5dec-4529-8e62-3849e8dcadd2")
@EFapsRevision("$Rev$")
public abstract class Inventory_Base
    extends AbstractCommon
{

    private Boolean showStorage = null;

    private List<Instance> storageInsts = new ArrayList<>();

    private boolean evaluateCost = false;

    /**
     * @param _parameter
     * @return
     * @throws EFapsException
     */
    public List<InventoryBean> getInventory(final Parameter _parameter)
        throws EFapsException
    {
        final List<InventoryBean> ret = new ArrayList<>();
        final Set<Instance> prodInsts = new HashSet<>();

        final Map<Instance, InventoryBean> map = new HashMap<>();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Inventory);
        add2QueryBuilder(_parameter, queryBldr);
        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder selStorage = SelectBuilder.get().linkto(CIProducts.Inventory.Storage);
        final SelectBuilder selStorageInst = new SelectBuilder(selStorage).instance();
        final SelectBuilder selStorageName = new SelectBuilder(selStorage).attribute(CIProducts.StorageAbstract.Name);
        if (isShowStorage()) {
            multi.addSelect(selStorageInst, selStorageName);
        }
        final SelectBuilder selProd = SelectBuilder.get().linkto(CIProducts.Inventory.Product);
        final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
        final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Description);
        multi.addSelect(selProdInst, selProdName, selProdDescr);
        multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.UoM, CIProducts.Inventory.Reserved);
        multi.execute();
        while (multi.next()) {
            final InventoryBean bean;
            if (isShowStorage()) {
                bean = getBean(_parameter);
                bean.setStorage(multi.<String>getSelect(selStorageName));
                bean.setProdInstance(multi.<Instance>getSelect(selProdInst));
                bean.setProdDescr(multi.<String>getSelect(selProdDescr));
                bean.setProdName(multi.<String>getSelect(selProdName));
                bean.setUoM(Dimension.getUoM(multi.<Long>getAttribute(CIProducts.Inventory.UoM)));
                ret.add(bean);
            } else {
                final Instance prodInst = multi.getSelect(selProdInst);
                if (map.containsKey(prodInst)) {
                    bean = map.get(prodInst);
                } else {
                    bean = getBean(_parameter);
                    bean.setProdInstance(multi.<Instance>getSelect(selProdInst));
                    bean.setProdDescr(multi.<String>getSelect(selProdDescr));
                    bean.setProdName(multi.<String>getSelect(selProdName));
                    bean.setUoM(Dimension.getUoM(multi.<Long>getAttribute(CIProducts.Inventory.UoM)));
                    map.put(prodInst, bean);
                }
            }
            bean.addQuantity(multi.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity));
            bean.addReserved(multi.<BigDecimal>getAttribute(CIProducts.Inventory.Reserved));
            prodInsts.add(multi.<Instance>getSelect(selProdInst));
        }

        if (!isShowStorage()) {
            ret.addAll(map.values());
        }

        addCost(_parameter, ret, prodInsts);
        return ret;
    }

    protected void addCost(final Parameter _parameter,
                           final List<InventoryBean> _beans,
                           final Set<Instance> _prodInsts)
        throws EFapsException
    {
        if (isEvaluateCost()) {
            final Map<Instance, CostBean> costs = getCostObject(_parameter)
                            .getCosts(_parameter, _prodInsts.toArray(new Instance[_prodInsts.size()]));
            for (final InventoryBean bean : _beans) {
                bean.setCostBean(costs.get(bean.getProdInstance()));
            }
        }
    }


    protected Cost getCostObject(final Parameter _parameter)
    {
        return new Cost();
    }

    protected void add2QueryBuilder(final Parameter _parameter,
                                    final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StorageAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.StorageAbstract.StatusAbstract,
                        Status.find(CIProducts.StorageAbstractStatus.Active));
        _queryBldr.addWhereAttrInQuery(CIProducts.Inventory.Storage,
                        queryBldr.getAttributeQuery(CIProducts.StorageAbstract.ID));

        if (!getStorageInsts().isEmpty()) {
            _queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, getStorageInsts().toArray());
        }
    }

    protected InventoryBean getBean(final Parameter _parameter)
    {
        return new InventoryBean();
    }

    /**
     * Getter method for the instance variable {@link #showStorage}.
     *
     * @return value of instance variable {@link #showStorage}
     */
    public boolean isShowStorage()
    {
        if (this.showStorage == null) {
            setShowStorage(!getStorageInsts().isEmpty());
        }
        return this.showStorage;
    }

    /**
     * Setter method for instance variable {@link #showStorage}.
     *
     * @param _showStorage value for instance variable {@link #showStorage}
     */
    public void setShowStorage(final boolean _showStorage)
    {
        this.showStorage = _showStorage;
    }

    /**
     * Getter method for the instance variable {@link #storageInsts}.
     *
     * @return value of instance variable {@link #storageInsts}
     */
    public List<Instance> getStorageInsts()
    {
        return this.storageInsts;
    }

    /**
     * Setter method for instance variable {@link #storageInsts}.
     *
     * @param _storageInsts value for instance variable {@link #storageInsts}
     */
    public void setStorageInsts(final List<Instance> _storageInsts)
    {
        this.storageInsts = _storageInsts;
    }

    /**
     * Getter method for the instance variable {@link #evaluateCost}.
     *
     * @return value of instance variable {@link #evaluateCost}
     */
    public boolean isEvaluateCost()
    {
        return this.evaluateCost;
    }

    /**
     * Setter method for instance variable {@link #evaluateCost}.
     *
     * @param _evaluateCost value for instance variable {@link #evaluateCost}
     */
    public void setEvaluateCost(final boolean _evaluateCost)
    {
        this.evaluateCost = _evaluateCost;
    }

    public static class InventoryBean
    {

        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal reserved = BigDecimal.ZERO;
        private UoM uoM;

        private Instance prodInstance;
        private String prodName;
        private String prodDescr;

        private String storage;

        private BigDecimal cost = BigDecimal.ZERO;
        private String currency = "";

        private CostBean costBean;

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getTotal()
        {
            return getCost().multiply(getQuantity());
        }

        /**
         * @param _costBean
         */
        public void setCostBean(final CostBean _costBean)
            throws EFapsException
        {
            if (_costBean != null) {
                this.costBean = _costBean;
                this.cost = getCostBean().getCost();
                this.currency = CurrencyInst.get(getCostBean().getCurrencyInstance()).getName();
            }
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return this.quantity;
        }

        /**
         * @param _attribute
         */
        public void addReserved(final BigDecimal _reserved)
        {
            this.reserved = this.reserved.add(_reserved);

        }

        /**
         * @param _attribute
         */
        public void addQuantity(final BigDecimal _quantity)
        {
            this.quantity = this.quantity.add(_quantity);
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         */
        public void setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
        }

        /**
         * Getter method for the instance variable {@link #reserved}.
         *
         * @return value of instance variable {@link #reserved}
         */
        public BigDecimal getReserved()
        {
            return this.reserved;
        }

        /**
         * Setter method for instance variable {@link #reserved}.
         *
         * @param _reserved value for instance variable {@link #reserved}
         */
        public void setReserved(final BigDecimal _reserved)
        {
            this.reserved = _reserved;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public String getUoM()
        {
            return this.uoM.getName();
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         */
        public void setUoM(final UoM _uoM)
        {
            this.uoM = _uoM;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            return this.prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         */
        public void setProdName(final String _prodName)
        {
            this.prodName = _prodName;
        }

        /**
         * Getter method for the instance variable {@link #prodDescr}.
         *
         * @return value of instance variable {@link #prodDescr}
         */
        public String getProdDescr()
        {
            return this.prodDescr;
        }

        /**
         * Setter method for instance variable {@link #prodDescr}.
         *
         * @param _prodDescr value for instance variable {@link #prodDescr}
         */
        public void setProdDescr(final String _prodDescr)
        {
            this.prodDescr = _prodDescr;
        }

        /**
         * Getter method for the instance variable {@link #storage}.
         *
         * @return value of instance variable {@link #storage}
         */
        public String getStorage()
        {
            return this.storage;
        }

        /**
         * Setter method for instance variable {@link #storage}.
         *
         * @param _storage value for instance variable {@link #storage}
         */
        public void setStorage(final String _storage)
        {
            this.storage = _storage;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost()
        {
            return this.cost;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         */
        public void setCost(final BigDecimal _cost)
        {
            this.cost = _cost;
        }

        /**
         * Getter method for the instance variable {@link #currency}.
         *
         * @return value of instance variable {@link #currency}
         */
        public String getCurrency()
        {
            return this.currency;
        }

        /**
         * Setter method for instance variable {@link #currency}.
         *
         * @param _currency value for instance variable {@link #currency}
         */
        public void setCurrency(final String _currency)
        {
            this.currency = _currency;
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public Instance getProdInstance()
        {
            return this.prodInstance;
        }

        /**
         * Setter method for instance variable {@link #prodInstance}.
         *
         * @param _prodInstance value for instance variable
         *            {@link #prodInstance}
         */
        public void setProdInstance(final Instance _prodInstance)
        {
            this.prodInstance = _prodInstance;
        }

        /**
         * Getter method for the instance variable {@link #costBean}.
         *
         * @return value of instance variable {@link #costBean}
         */
        public CostBean getCostBean()
        {
            return this.costBean;
        }
    }
}
