/*
 * Copyright 2003 - 2015 The eFaps Team
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f2ec6675-5dec-4529-8e62-3849e8dcadd2")
@EFapsApplication("eFapsApp-Products")
public abstract class Inventory_Base
    extends AbstractCommon
{

    /**
     * Show storage information. used as a Tristate.
     */
    private Boolean showStorage;

    /**
     * Show the classification in the report.
     */
    private boolean showProdClass;

    /**
     * Currency the cost evaluation will be executed for.
     * If null no cost evaluation is done.
     */
    private Instance currencyInst;

    /**
     * Instances of storages included in the inventory.
     */
    private List<Instance> storageInsts = new ArrayList<>();

    /**
     * Date of the inventory. used as a Tristate.
     */
    private DateTime date;

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return list of beans
     * @throws EFapsException on error
     */
    public List<? extends InventoryBean> getInventory(final Parameter _parameter)
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
        final SelectBuilder selProdClass = new SelectBuilder(selProd).clazz().type();
        final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Description);
        if (isShowProdClass()) {
            multi.addSelect(selProdClass);
        }
        multi.addSelect(selProdClass, selProdInst, selProdName, selProdDescr);
        multi.addAttribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.UoM, CIProducts.Inventory.Reserved);
        multi.execute();
        while (multi.next()) {
            final InventoryBean bean;
            if (isShowStorage()) {
                bean = getBean(_parameter);
                bean.setStorage(multi.<String>getSelect(selStorageName));
                bean.setStorageInstance(multi.<Instance>getSelect(selStorageInst));
                bean.setProdInstance(multi.<Instance>getSelect(selProdInst));
                bean.setProdDescr(multi.<String>getSelect(selProdDescr));
                bean.setProdName(multi.<String>getSelect(selProdName));
                bean.setUoM(Dimension.getUoM(multi.<Long>getAttribute(CIProducts.Inventory.UoM)));
                if (isShowProdClass()) {
                    bean.setProdClasslist(multi.<List<Classification>>getSelect(selProdClass));
                }
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
                    if (isShowProdClass()) {
                        bean.setProdClasslist(multi.<List<Classification>>getSelect(selProdClass));
                    }
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

        calulateInventory(_parameter, ret, prodInsts);
        if (isCalculateInventory()) {
            CollectionUtils.filter(ret, new EmptyPredicate());
        }
        addCost(_parameter, ret, prodInsts);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _beans list of inventory beans
     * @param _prodInsts set of product instances
     * @throws EFapsException on error
     */
    protected void calulateInventory(final Parameter _parameter,
                                     final List<InventoryBean> _beans,
                                     final Set<Instance> _prodInsts)
        throws EFapsException
    {
        if (isCalculateInventory()) {

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
            add2QueryBuilder4Transaction(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();

            final SelectBuilder selStorageInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Storage)
                            .instance();
            if (isShowStorage()) {
                multi.addSelect(selStorageInst);
            }

            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product)
                            .instance();
            multi.addSelect(selProdInst);
            multi.addAttribute(CIProducts.TransactionAbstract.UoM, CIProducts.TransactionAbstract.Quantity);
            multi.execute();
            final Map<Instance, Set<TransactionBean>> prodInst2beans = new HashMap<>();
            while (multi.next()) {
                final TransactionBean bean = getTransactionBean(_parameter);
                final Instance prodInst = multi.<Instance>getSelect(selProdInst);
                bean.setInstance(multi.getCurrentInstance())
                                .setProdInstance(prodInst)
                                .setQuantity(multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity))
                                .setUoM(Dimension.getUoM(multi
                                                .<Long>getAttribute(CIProducts.TransactionAbstract.UoM)));
                if (isShowStorage()) {
                    bean.setStorageInstance(multi.<Instance>getSelect(selStorageInst));
                }
                Set<TransactionBean> beans;
                if (prodInst2beans.containsKey(prodInst)) {
                    beans = prodInst2beans.get(prodInst);
                } else {
                    beans = new HashSet<>();
                    prodInst2beans.put(prodInst, beans);
                }
                beans.add(bean);
            }

            for (final Entry<Instance, Set<TransactionBean>> entry : prodInst2beans.entrySet()) {
                final ProductPredicate predicate = new ProductPredicate(entry.getKey());
                // filter the Collection
                final Collection<InventoryBean> inventoryBeans = CollectionUtils.select(_beans, predicate);
                if (isShowStorage()) {
                    for (final TransactionBean bean : entry.getValue()) {
                        final StoragePredicate storagePredicate = new StoragePredicate(bean.getStorageInstance());
                        final Collection<InventoryBean> subInventoryBeans = CollectionUtils.select(inventoryBeans,
                                        storagePredicate);
                        final InventoryBean inventoryBean;
                        if (subInventoryBeans.isEmpty()) {
                            inventoryBean = getBean(_parameter);
                            inventoryBean.setProdInstance(entry.getKey());
                            inventoryBean.setStorageInstance(bean.getStorageInstance());
                            inventoryBeans.add(inventoryBean);
                            _beans.add(inventoryBean);
                        } else {
                            inventoryBean = subInventoryBeans.iterator().next();
                        }
                        inventoryBean.addTransaction(bean);
                    }
                } else {
                    final InventoryBean inventoryBean;
                    if (inventoryBeans.isEmpty()) {
                        inventoryBean = getBean(_parameter);
                        inventoryBean.setProdInstance(entry.getKey());
                        inventoryBeans.add(inventoryBean);
                        _beans.add(inventoryBean);
                    } else {
                        inventoryBean = inventoryBeans.iterator().next();
                    }
                    for (final TransactionBean bean : entry.getValue()) {
                        inventoryBean.addTransaction(bean);
                    }
                }
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr QueryBuilder to add to
     * @throws EFapsException on error
     */
    protected void add2QueryBuilder4Transaction(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
        throws EFapsException
    {
        if (!getStorageInsts().isEmpty()) {
            _queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Storage, getStorageInsts().toArray());
        }
        _queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date, getDate().minusMinutes(1));
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _beans list of inventory beans
     * @param _prodInsts set of product instances
     * @throws EFapsException on error
     */
    protected void addCost(final Parameter _parameter,
                           final List<InventoryBean> _beans,
                           final Set<Instance> _prodInsts)
        throws EFapsException
    {
        if (isEvaluateCost()) {
            final Map<Instance, CostBean> costs = getCostObject(_parameter)
                            .getCosts(_parameter, getDate() == null ? new DateTime() : getDate(),
                                            _prodInsts.toArray(new Instance[_prodInsts.size()]));
            for (final InventoryBean bean : _beans) {
                bean.setCostBean(_parameter, costs.get(bean.getProdInstance()), getCurrencyInst());
            }
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Cost instance
     * @throws EFapsException on error
     */
    protected Cost getCostObject(final Parameter _parameter)

        throws EFapsException
    {
        return new Cost();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr QueryBuilder to add to
     * @throws EFapsException on error
     */
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

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new InventoryBean instance
     * @throws EFapsException on error
     */
    protected InventoryBean getBean(final Parameter _parameter)
        throws EFapsException
    {
        return new InventoryBean();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new TransactionBean instance
     * @throws EFapsException on error
     */
    protected TransactionBean getTransactionBean(final Parameter _parameter)
        throws EFapsException
    {
        return new TransactionBean();
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
        return getCurrencyInst() != null && this.currencyInst.isValid();
    }

    /**
     * Setter method for instance variable {@link #currencyInst}.
     *
     * @param _evaluateCost value for instance variable {@link #currencyInst}
     */
    public void setCurrencyInst(final Instance _currencyInst)
    {
        this.currencyInst = _currencyInst;
    }

    /**
     * Getter method for the instance variable {@link #currencyInst}.
     *
     * @return value of instance variable {@link #currencyInst}
     */
    public Instance getCurrencyInst()
    {
        return this.currencyInst;
    }

    /**
     * Getter method for the instance variable {@link #showProdClass}.
     *
     * @return value of instance variable {@link #showProdClass}
     */
    public boolean isShowProdClass()
    {
        return this.showProdClass;
    }

    /**
     * Setter method for instance variable {@link #showProdClass}.
     *
     * @param _showProdClass value for instance variable {@link #showProdClass}
     */
    public void setShowProdClass(final boolean _showProdClass)
    {
        this.showProdClass = _showProdClass;
    }

    /**
     * @return true if inventory must be calculated else false
     */
    public boolean isCalculateInventory()
    {
        return this.date != null && this.date.toLocalDate().isBefore(DateTime.now().toLocalDate());
    }

    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     */
    public DateTime getDate()
    {
        return this.date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     */
    public void setDate(final DateTime _date)
    {
        this.date = _date;
    }

    public static class EmptyPredicate
        implements Predicate<InventoryBean>
    {

        @Override
        public boolean evaluate(final InventoryBean _inventoryBean)
        {
            return _inventoryBean.getQuantity().compareTo(BigDecimal.ZERO) != 0
                            || _inventoryBean.getReserved().compareTo(BigDecimal.ZERO) != 0;
        }
    }

    public static class ProductPredicate
        implements Predicate<InventoryBean>
    {

        private final Instance productInstance;

        /**
         * @param _key
         */
        public ProductPredicate(final Instance _productInstance)
        {
            this.productInstance = _productInstance;
        }

        @Override
        public boolean evaluate(final InventoryBean _inventoryBean)
        {
            return this.productInstance.equals(_inventoryBean.getProdInstance());
        }
    }

    public static class StoragePredicate
        implements Predicate<InventoryBean>
    {

        private final Instance storageInstance;

        /**
         * @param _key
         */
        public StoragePredicate(final Instance _storageInstance)
        {
            this.storageInstance = _storageInstance;
        }

        @Override
        public boolean evaluate(final InventoryBean _inventoryBean)
        {
            return this.storageInstance.equals(_inventoryBean.getStorageInstance());
        }
    }


    public static class TransactionBean
    {

        private Instance instance;
        private BigDecimal quantity = BigDecimal.ZERO;
        private UoM uoM;

        private Instance prodInstance;

        private Instance storageInstance;

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return this.instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         */
        public TransactionBean setInstance(final Instance _instance)
        {
            this.instance = _instance;
            return this;
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
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         */
        public TransactionBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public UoM getUoM()
        {
            return this.uoM;
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         */
        public TransactionBean setUoM(final UoM _uoM)
        {
            this.uoM = _uoM;
            return this;
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
        public TransactionBean setProdInstance(final Instance _prodInstance)
        {
            this.prodInstance = _prodInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #storageInstance}.
         *
         * @return value of instance variable {@link #storageInstance}
         */
        public Instance getStorageInstance()
        {
            return this.storageInstance;
        }

        /**
         * Setter method for instance variable {@link #storageInstance}.
         *
         * @param _storageInstance value for instance variable
         *            {@link #storageInstance}
         */
        public TransactionBean setStorageInstance(final Instance _storageInstance)
        {
            this.storageInstance = _storageInstance;
            return this;
        }
    }

    public static class InventoryBean
    {

        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal reserved = BigDecimal.ZERO;
        private UoM uoM;

        private Instance prodInstance;
        private String prodName;
        private String prodDescr;

        private Instance storageInstance;

        private String storage;

        private BigDecimal cost = BigDecimal.ZERO;
        private String currency = "";

        private CostBean costBean;

        private List<Classification> prodClasslist;

        protected void initialize()
        {
            try {
                if (getProdInstance() != null && getProdInstance().isValid() && this.prodName == null) {
                    final PrintQuery print = CachedPrintQuery.get4Request(getProdInstance());
                    print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                                    CIProducts.ProductAbstract.Dimension);
                    print.execute();
                    setProdName(print.<String>getAttribute(CIProducts.ProductAbstract.Name));
                    setProdDescr(print.<String>getAttribute(CIProducts.ProductAbstract.Description));
                    setUoM(Dimension.get(print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension)).getBaseUoM());
                }

                if (getStorageInstance() != null && getStorageInstance().isValid() && this.storage == null) {
                    final PrintQuery print = CachedPrintQuery.get4Request(getStorageInstance());
                    print.addAttribute(CIProducts.StorageAbstract.Name);
                    print.execute();
                    setStorage(print.<String>getAttribute(CIProducts.StorageAbstract.Name));
                }
            } catch (final Exception e) {
                // TODO: handle exception
            }
        }

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
        public void setCostBean(final Parameter _parameter,
                                final CostBean _costBean,
                                final Instance _currencyInst)
            throws EFapsException
        {
            if (_costBean != null) {
                this.costBean = _costBean;
                if (_currencyInst != null && _currencyInst.isValid()) {
                    this.currency = CurrencyInst.get(_currencyInst).getSymbol();
                    this.cost = getCostBean().getCost4Currency(_parameter, _currencyInst);
                } else {
                    this.currency = CurrencyInst.get(getCostBean().getCurrencyInstance()).getSymbol();
                    this.cost = getCostBean().getCost();
                }
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
         * @param _bean
         */
        public void addTransaction(final TransactionBean _bean)
        {
            if (_bean.getInstance().getType().isCIType(CIProducts.TransactionInbound)) {
                addQuantity(_bean.getQuantity().negate());
            } else if (_bean.getInstance().getType().isCIType(CIProducts.TransactionOutbound)) {
                addQuantity(_bean.getQuantity());
            } else if (_bean.getInstance().getType().isCIType(CIProducts.TransactionReservationInbound)) {
                addReserved(_bean.getQuantity().negate());
            } else if (_bean.getInstance().getType().isCIType(CIProducts.TransactionReservationOutbound)) {
                addReserved(_bean.getQuantity());
            }
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
            initialize();
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
            initialize();
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
            initialize();
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
            initialize();
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

        public String getProdType()
        {
            return getProdInstance().getType().getLabel();
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public String getProdOID()
        {
            return getProdInstance().getOid();
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

        /**
         * @return
         */
        public String getProdClass()
            throws CacheReloadException
        {
            final StringBuilder ret = new StringBuilder();
            if (getProdClasslist() != null && !getProdClasslist().isEmpty()) {
                for (final Classification clazz : getProdClasslist()) {
                    Classification clazzTmp = clazz;
                    while (!clazzTmp.isRoot()) {
                        if (ret.length() == 0) {
                            ret.append(clazz.getLabel());
                        } else {
                            ret.insert(0, clazzTmp.getLabel() + " - ");
                        }
                        clazzTmp = clazzTmp.getParentClassification();
                    }
                    if (ret.length() == 0) {
                        ret.append(clazz.getLabel());
                    }
                }
            } else {
                ret.append("-");
            }
            return ret.toString();
        }

        /**
         * Getter method for the instance variable {@link #prodClasslist}.
         *
         * @return value of instance variable {@link #prodClasslist}
         */
        public List<Classification> getProdClasslist()
        {
            initialize();
            return this.prodClasslist;
        }

        /**
         * Setter method for instance variable {@link #prodClasslist}.
         *
         * @param _prodClasslist value for instance variable
         *            {@link #prodClasslist}
         */
        public void setProdClasslist(final List<Classification> _prodClasslist)
        {
            this.prodClasslist = _prodClasslist;
        }

        /**
         * Getter method for the instance variable {@link #storageInstance}.
         *
         * @return value of instance variable {@link #storageInstance}
         */
        public Instance getStorageInstance()
        {
            return this.storageInstance;
        }

        /**
         * Setter method for instance variable {@link #storageInstance}.
         *
         * @param _storageInstance value for instance variable
         *            {@link #storageInstance}
         */
        public void setStorageInstance(final Instance _storageInstance)
        {
            this.storageInstance = _storageInstance;
        }
    }
}
