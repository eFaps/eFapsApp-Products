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
import java.util.Arrays;
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
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIMsgProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.eql.ClassSelect;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Logger for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    /**
     * Show storage information. used as a Tristate.
     */
    private Boolean showStorage;

    /**
     * Show the classification in the report.
     */
    private boolean showProdClass;

    /** Use alternative type for costing. */
    private Instance alternativeCurrencyInst;

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

    private boolean forceDate = false;
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

        final QueryBuilder queryBldr = new QueryBuilder(getInventoryType(_parameter));
        add2QueryBuilder(_parameter, queryBldr);
        final MultiPrintQuery multi = queryBldr.getPrint();

        final SelectBuilder selStorage = SelectBuilder.get().linkto(CIProducts.InventoryAbstract.Storage);
        final SelectBuilder selStorageInst = new SelectBuilder(selStorage).instance();
        final SelectBuilder selStorageName = new SelectBuilder(selStorage).attribute(CIProducts.StorageAbstract.Name);
        if (isShowStorage()) {
            multi.addSelect(selStorageInst, selStorageName);
        }
        final SelectBuilder selProd = SelectBuilder.get().linkto(CIProducts.InventoryAbstract.Product);
        final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
        final SelectBuilder selProdClass = new SelectBuilder(selProd).clazz().type();
        final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Description);
        if (isShowProdClass()) {
            multi.addSelect(selProdClass);
        }
        multi.addSelect(selProdInst, selProdName, selProdDescr);
        multi.addMsgPhrase(selProd, CIMsgProducts.SlugMsgPhrase);
        multi.addAttribute(CIProducts.InventoryAbstract.Quantity, CIProducts.InventoryAbstract.UoM,
                        CIProducts.InventoryAbstract.Reserved);
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
                bean.setProdSlug(multi.getMsgPhrase(selProd, CIMsgProducts.SlugMsgPhrase));
                bean.setUoM(Dimension.getUoM(multi.<Long>getAttribute(CIProducts.InventoryAbstract.UoM)));
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
                    bean.setProdSlug(multi.getMsgPhrase(selProd, CIMsgProducts.SlugMsgPhrase));
                    bean.setUoM(Dimension.getUoM(multi.<Long>getAttribute(CIProducts.InventoryAbstract.UoM)));
                    if (isShowProdClass()) {
                        bean.setProdClasslist(multi.<List<Classification>>getSelect(selProdClass));
                    }
                    map.put(prodInst, bean);
                }
            }
            bean.addQuantity(multi.<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Quantity));
            bean.addReserved(multi.<BigDecimal>getAttribute(CIProducts.InventoryAbstract.Reserved));
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

            final QueryBuilder queryBldr = new QueryBuilder(getTransactionType(_parameter));
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
                _prodInsts.add(prodInst);
                bean.setInstance(multi.getCurrentInstance())
                                .setProdInstance(prodInst)
                                .setQuantity(multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity))
                                .setUoM(Dimension.getUoM(multi
                                                .<Long>getAttribute(CIProducts.TransactionAbstract.UoM)));
                if (isShowStorage()) {
                    bean.setStorageInstance(multi.<Instance>getSelect(selStorageInst));
                }
                final Set<TransactionBean> beans;
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
        // if not forced Date
        // transaction are all exactly at 00:00 date ==> all transaction up to
        // this date must be included
        _queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date,
                        forceDate ? getDate() : getDate().plusDays(1).minusMinutes(1));
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
            final Map<Instance, CostBean> costs;
            if (isAlternative()) {
                costs = getCostObject(_parameter).getAlternativeCosts(_parameter,
                                getDate() == null ? new DateTime() : getDate(), getAlternativeCurrencyInst(),
                                _prodInsts.toArray(new Instance[_prodInsts.size()]));
            } else {
                costs = getCostObject(_parameter)
                                .getCosts(_parameter, getDate() == null ? new DateTime() : getDate(),
                                                _prodInsts.toArray(new Instance[_prodInsts.size()]));
            }
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
        _queryBldr.addWhereAttrInQuery(CIProducts.InventoryAbstract.Storage,
                        queryBldr.getAttributeQuery(CIProducts.StorageAbstract.ID));

        if (!getStorageInsts().isEmpty()) {
            _queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Storage, getStorageInsts().toArray());
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
     * Gets the type for storage.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the type4 storage
     * @throws EFapsException on error
     */
    protected Type getInventoryType(final Parameter _parameter)
        throws EFapsException
    {
        return CIProducts.InventoryAbstract.getType();
    }

    /**
     * Gets the type for storage.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the type4 storage
     * @throws EFapsException on error
     */
    protected Type getTransactionType(final Parameter _parameter)
        throws EFapsException
    {
        return CIProducts.TransactionAbstract.getType();
    }

    /**
     * Getter method for the instance variable {@link #showStorage}.
     *
     * @return value of instance variable {@link #showStorage}
     */
    public boolean isShowStorage()
    {
        if (showStorage == null) {
            setShowStorage(!getStorageInsts().isEmpty());
        }
        return showStorage;
    }

    /**
     * Setter method for instance variable {@link #showStorage}.
     *
     * @param _showStorage value for instance variable {@link #showStorage}
     */
    public void setShowStorage(final boolean _showStorage)
    {
        showStorage = _showStorage;
    }

    /**
     * Getter method for the instance variable {@link #storageInsts}.
     *
     * @return value of instance variable {@link #storageInsts}
     */
    public List<Instance> getStorageInsts()
    {
        return storageInsts;
    }

    /**
     * Setter method for instance variable {@link #storageInsts}.
     *
     * @param _storageInsts value for instance variable {@link #storageInsts}
     * @return the inventory
     */
    public Inventory setStorageInsts(final List<Instance> _storageInsts)
    {
        storageInsts = _storageInsts;
        return (Inventory) this;
    }

    /**
     * Getter method for the instance variable {@link #evaluateCost}.
     *
     * @return value of instance variable {@link #evaluateCost}
     */
    public boolean isEvaluateCost()
    {
        return getCurrencyInst() != null && currencyInst.isValid();
    }

    /**
     * Setter method for instance variable {@link #currencyInst}.
     *
     * @param _currencyInst the new currency inst
     * @return the inventory
     */
    public Inventory setCurrencyInst(final Instance _currencyInst)
    {
        currencyInst = _currencyInst;
        return (Inventory) this;
    }

    /**
     * Getter method for the instance variable {@link #currencyInst}.
     *
     * @return value of instance variable {@link #currencyInst}
     */
    public Instance getCurrencyInst()
    {
        return currencyInst;
    }

    /**
     * Getter method for the instance variable {@link #showProdClass}.
     *
     * @return value of instance variable {@link #showProdClass}
     */
    public boolean isShowProdClass()
    {
        return showProdClass;
    }

    /**
     * Setter method for instance variable {@link #showProdClass}.
     *
     * @param _showProdClass value for instance variable {@link #showProdClass}
     * @return the inventory
     */
    public Inventory setShowProdClass(final boolean _showProdClass)
    {
        showProdClass = _showProdClass;
        return (Inventory) this;
    }

    /**
     * Getter method for the instance variable {@link #alternative}.
     *
     * @return value of instance variable {@link #alternative}
     */
    public boolean isAlternative()
    {
        return getAlternativeCurrencyInst() != null && getAlternativeCurrencyInst().isValid();
    }

    /**
     * Getter method for the instance variable {@link #alternativeCurrencyInst}.
     *
     * @return value of instance variable {@link #alternativeCurrencyInst}
     */
    public Instance getAlternativeCurrencyInst()
    {
        return alternativeCurrencyInst;
    }

    /**
     * Setter method for instance variable {@link #alternativeCurrencyInst}.
     *
     * @param _alternativeCurrencyInst value for instance variable {@link #alternativeCurrencyInst}
     * @return the inventory
     */
    public Inventory setAlternativeCurrencyInst(final Instance _alternativeCurrencyInst)
    {
        alternativeCurrencyInst = _alternativeCurrencyInst;
        return (Inventory) this;
    }

    /**
     * @return true if inventory must be calculated else false
     */
    public boolean isCalculateInventory()
    {
        return date != null && date.toLocalDate().isBefore(DateTime.now().toLocalDate());
    }

    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     */
    public DateTime getDate()
    {
        return date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     * @return the inventory
     */
    public Inventory setDate(final DateTime _date)
    {
        date = _date;
        return (Inventory) this;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     * @return the inventory
     */
    public Inventory setDate(final DateTime _date, final boolean forceDate)
    {
        this.forceDate = forceDate;
        return setDate(_date);
    }

    /**
     * Gets the inventory for product.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _prodInstance the _prod instance
     * @return the inventory4 product
     * @throws EFapsException on error
     */
    protected static InventoryBean getInventory4Product(final Parameter _parameter,
                                                        final Instance _storageInst,
                                                        final Instance _prodInstance)
        throws EFapsException
    {
        return Inventory_Base.getInventory4Product(_parameter, _storageInst, new DateTime(), _prodInstance);
    }

    /**
     * Gets the inventory for product.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _date the _date
     * @param _prodInstance the _prod instance
     * @return the inventory4 product
     * @throws EFapsException on error
     */
    protected static InventoryBean getInventory4Product(final Parameter _parameter,
                                                        final Instance _storageInst,
                                                        final DateTime _date,
                                                        final Instance _prodInstance)
        throws EFapsException
    {
        final Map<Instance, InventoryBean> map = Inventory.getInventory4Products(_parameter, _storageInst, _date,
                        _prodInstance);
        return map.get(_prodInstance);
    }

    /**
     * Gets the inventory 4 products.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _prodInstances the _prod instances
     * @return the inventory4 products
     * @throws EFapsException on error
     */
    protected static Map<Instance, InventoryBean> getInventory4Products(final Parameter _parameter,
                                                                        final Instance _storageInst,
                                                                        final Instance... _prodInstances)
        throws EFapsException
    {
        return Inventory.getInventory4Products(_parameter, _storageInst, new DateTime(), _prodInstances);
    }

    /**
     * Gets the inventory 4 products.
     *
     * @param _parameter the _parameter
     * @param _storageInst the _storage inst
     * @param _date the _date
     * @param _prodInstances the _prod instances
     * @return the inventory4 products
     * @throws EFapsException on error
     */
    protected static Map<Instance, InventoryBean> getInventory4Products(final Parameter _parameter,
                                                                        final Instance _storageInst,
                                                                        final DateTime _date,
                                                                        final Instance... _prodInstances)
        throws EFapsException
    {
        final Map<Instance, InventoryBean> ret = new HashMap<>();

        final Inventory inventory = new Inventory()
        {
            @Override
            protected void add2QueryBuilder(final Parameter _parameter,
                                            final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBuilder(_parameter, _queryBldr);
                _queryBldr.addWhereAttrEqValue(CIProducts.InventoryAbstract.Product, (Object[]) _prodInstances);
            }

            @Override
            protected void add2QueryBuilder4Transaction(final Parameter _parameter,
                                                        final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBuilder4Transaction(_parameter, _queryBldr);
                _queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, (Object[]) _prodInstances);
            }
        };
        inventory.setDate(_date.withTimeAtStartOfDay());
        inventory.setShowStorage(false);
        if (InstanceUtils.isValid(_storageInst)) {
            inventory.setStorageInsts(Arrays.asList(_storageInst));
        }

        final List<? extends InventoryBean> list = inventory.getInventory(_parameter);
        for (final InventoryBean bean : list) {
            ret.put(bean.getProdInstance(), bean);
        }
        return ret;
    }

    /**
     * The Class EmptyPredicate.
     *
     */
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

    /**
     * The Class ProductPredicate.
     *
     * @author The eFaps Team
     */
    public static class ProductPredicate
        implements Predicate<InventoryBean>
    {

        /** The product instance. */
        private final Instance productInstance;

        /**
         * Instantiates a new product predicate.
         *
         * @param _productInstance the _product instance
         */
        public ProductPredicate(final Instance _productInstance)
        {
            productInstance = _productInstance;
        }

        @Override
        public boolean evaluate(final InventoryBean _inventoryBean)
        {
            return productInstance.equals(_inventoryBean.getProdInstance());
        }
    }

    /**
     * The Class StoragePredicate.
     */
    public static class StoragePredicate
        implements Predicate<InventoryBean>
    {

        /** The storage instance. */
        private final Instance storageInstance;

        /**
         * Instantiates a new storage predicate.
         *
         * @param _storageInstance the _storage instance
         */
        public StoragePredicate(final Instance _storageInstance)
        {
            storageInstance = _storageInstance;
        }

        @Override
        public boolean evaluate(final InventoryBean _inventoryBean)
        {
            return storageInstance.equals(_inventoryBean.getStorageInstance());
        }
    }


    /**
     * The Class TransactionBean.
     */
    public static class TransactionBean
    {

        /** The instance. */
        private Instance instance;

        /** The quantity. */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** The uo m. */
        private UoM uoM;

        /** The prod instance. */
        private Instance prodInstance;

        /** The storage instance. */
        private Instance storageInstance;

        /**
         * Getter method for the instance variable {@link #instance}.
         *
         * @return value of instance variable {@link #instance}
         */
        public Instance getInstance()
        {
            return instance;
        }

        /**
         * Setter method for instance variable {@link #instance}.
         *
         * @param _instance value for instance variable {@link #instance}
         * @return the transaction bean
         */
        public TransactionBean setInstance(final Instance _instance)
        {
            instance = _instance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            return quantity;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         * @return the transaction bean
         */
        public TransactionBean setQuantity(final BigDecimal _quantity)
        {
            quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public UoM getUoM()
        {
            return uoM;
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         * @return the transaction bean
         */
        public TransactionBean setUoM(final UoM _uoM)
        {
            uoM = _uoM;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public Instance getProdInstance()
        {
            return prodInstance;
        }

        /**
         * Setter method for instance variable {@link #prodInstance}.
         *
         * @param _prodInstance value for instance variable
         *            {@link #prodInstance}
         * @return the transaction bean
         */
        public TransactionBean setProdInstance(final Instance _prodInstance)
        {
            prodInstance = _prodInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #storageInstance}.
         *
         * @return value of instance variable {@link #storageInstance}
         */
        public Instance getStorageInstance()
        {
            return storageInstance;
        }

        /**
         * Setter method for instance variable {@link #storageInstance}.
         *
         * @param _storageInstance value for instance variable
         *            {@link #storageInstance}
         * @return the transaction bean
         */
        public TransactionBean setStorageInstance(final Instance _storageInstance)
        {
            storageInstance = _storageInstance;
            return this;
        }
    }

    /**
     * The Class InventoryBean.
     */
    public static class InventoryBean
    {

        /** The quantity. */
        private BigDecimal quantity = BigDecimal.ZERO;

        /** The reserved. */
        private BigDecimal reserved = BigDecimal.ZERO;

        /** The uo m. */
        private UoM uoM;

        /** The prod instance. */
        private Instance prodInstance;

        /** The prod name. */
        private String prodName;

        private String prodSlug;

        /** The prod descr. */
        private String prodDescr;

        /** The storage instance. */
        private Instance storageInstance;

        /** The storage. */
        private String storage;

        /** The cost. */
        private BigDecimal cost = BigDecimal.ZERO;

        /** The currency. */
        private String currency = "";

        /** The cost bean. */
        private CostBean costBean;

        /** The prod classlist. */
        private List<Classification> prodClasslist;

        /**
         * Initialize.
         */
        protected void initialize()
        {
            try {
                if (getProdInstance() != null && getProdInstance().isValid() && prodName == null) {
                    final PrintQuery print = CachedPrintQuery.get4Request(getProdInstance());
                    print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                                    CIProducts.ProductAbstract.Dimension);
                    print.execute();
                    setProdName(print.<String>getAttribute(CIProducts.ProductAbstract.Name));
                    setProdDescr(print.<String>getAttribute(CIProducts.ProductAbstract.Description));
                    setUoM(Dimension.get(print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension)).getBaseUoM());
                }

                if (getStorageInstance() != null && getStorageInstance().isValid() && storage == null) {
                    final PrintQuery print = CachedPrintQuery.get4Request(getStorageInstance());
                    print.addAttribute(CIProducts.StorageAbstract.Name);
                    print.execute();
                    setStorage(print.<String>getAttribute(CIProducts.StorageAbstract.Name));
                }
            } catch (final EFapsException e) {
                LOG.error("EFapsException", e);
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
         * Sets the cost bean.
         *
         * @param _parameter the _parameter
         * @param _costBean the _cost bean
         * @param _currencyInst the _currency inst
         * @throws EFapsException on error
         */
        public void setCostBean(final Parameter _parameter,
                                final CostBean _costBean,
                                final Instance _currencyInst)
            throws EFapsException
        {
            if (_costBean != null) {
                costBean = _costBean;
                if (_currencyInst != null && _currencyInst.isValid()) {
                    currency = CurrencyInst.get(_currencyInst).getSymbol();
                    cost = getCostBean().getCost4Currency(_parameter, _currencyInst);
                } else {
                    currency = CurrencyInst.get(getCostBean().getCurrencyInstance()).getSymbol();
                    cost = getCostBean().getCost();
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
            return quantity;
        }

        /**
         * Adds the reserved.
         *
         * @param _reserved the _reserved
         */
        public void addReserved(final BigDecimal _reserved)
        {
            reserved = reserved.add(_reserved);

        }

        /**
         * Adds the quantity.
         *
         * @param _quantity the _quantity
         */
        public void addQuantity(final BigDecimal _quantity)
        {
            quantity = quantity.add(_quantity);
        }

        /**
         * Adds the transaction.
         *
         * @param _bean the _bean
         */
        public void addTransaction(final TransactionBean _bean)
        {
            if (_bean.getInstance().getType().isCIType(CIProducts.TransactionInbound)
                            || _bean.getInstance().getType().isCIType(CIProducts.TransactionIndividualInbound)) {
                addQuantity(_bean.getQuantity().negate());
            } else if (_bean.getInstance().getType().isCIType(CIProducts.TransactionOutbound)
                            || _bean.getInstance().getType().isCIType(CIProducts.TransactionIndividualOutbound)) {
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
            quantity = _quantity;
        }

        /**
         * Getter method for the instance variable {@link #reserved}.
         *
         * @return value of instance variable {@link #reserved}
         */
        public BigDecimal getReserved()
        {
            return reserved;
        }

        /**
         * Setter method for instance variable {@link #reserved}.
         *
         * @param _reserved value for instance variable {@link #reserved}
         */
        public void setReserved(final BigDecimal _reserved)
        {
            reserved = _reserved;
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public String getUoM()
        {
            initialize();
            return uoM.getName();
        }

        /**
         * Getter method for the instance variable {@link #uoM}.
         *
         * @return value of instance variable {@link #uoM}
         */
        public Long getUoMId()
        {
            initialize();
            return uoM.getId();
        }

        /**
         * Setter method for instance variable {@link #uoM}.
         *
         * @param _uoM value for instance variable {@link #uoM}
         */
        public void setUoM(final UoM _uoM)
        {
            uoM = _uoM;
        }

        /**
         * Getter method for the instance variable {@link #prodName}.
         *
         * @return value of instance variable {@link #prodName}
         */
        public String getProdName()
        {
            initialize();
            return prodName;
        }

        /**
         * Setter method for instance variable {@link #prodName}.
         *
         * @param _prodName value for instance variable {@link #prodName}
         */
        public void setProdName(final String _prodName)
        {
            prodName = _prodName;
        }

        public String getProdSlug()
        {
            return prodSlug;
        }


        public void setProdSlug(String prodSlug)
        {
            this.prodSlug = prodSlug;
        }

        /**
         * Getter method for the instance variable {@link #prodDescr}.
         *
         * @return value of instance variable {@link #prodDescr}
         */
        public String getProdDescr()
        {
            initialize();
            return prodDescr;
        }

        /**
         * Setter method for instance variable {@link #prodDescr}.
         *
         * @param _prodDescr value for instance variable {@link #prodDescr}
         */
        public void setProdDescr(final String _prodDescr)
        {
            prodDescr = _prodDescr;
        }

        /**
         * Getter method for the instance variable {@link #storage}.
         *
         * @return value of instance variable {@link #storage}
         */
        public String getStorage()
        {
            initialize();
            return storage;
        }

        /**
         * Setter method for instance variable {@link #storage}.
         *
         * @param _storage value for instance variable {@link #storage}
         */
        public void setStorage(final String _storage)
        {
            storage = _storage;
        }

        /**
         * Getter method for the instance variable {@link #cost}.
         *
         * @return value of instance variable {@link #cost}
         */
        public BigDecimal getCost()
        {
            return cost;
        }

        /**
         * Setter method for instance variable {@link #cost}.
         *
         * @param _cost value for instance variable {@link #cost}
         */
        public void setCost(final BigDecimal _cost)
        {
            cost = _cost;
        }

        /**
         * Getter method for the instance variable {@link #currency}.
         *
         * @return value of instance variable {@link #currency}
         */
        public String getCurrency()
        {
            return currency;
        }

        /**
         * Setter method for instance variable {@link #currency}.
         *
         * @param _currency value for instance variable {@link #currency}
         */
        public void setCurrency(final String _currency)
        {
            currency = _currency;
        }

        /**
         * Getter method for the instance variable {@link #prodInstance}.
         *
         * @return value of instance variable {@link #prodInstance}
         */
        public Instance getProdInstance()
        {
            return prodInstance;
        }

        /**
         * Gets the prod type.
         *
         * @return the prod type
         */
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
            prodInstance = _prodInstance;
        }

        /**
         * Getter method for the instance variable {@link #costBean}.
         *
         * @return value of instance variable {@link #costBean}
         */
        public CostBean getCostBean()
        {
            return costBean;
        }

        public String getProdFamily()
            throws EFapsException
        {
            return new ProductFamily().getName(ParameterUtil.instance(), prodInstance);
        }

        /**
         * Gets the prod class.
         *
         * @return the prod class
         * @throws EFapsException on error
         */
        public String getProdClass()
            throws EFapsException
        {
            return getProdClass(null);
        }

        /**
         * Gets the prod class.
         *
         * @param _level the level
         * @return the prod class
         * @throws EFapsException on error
         */
        public String getProdClass(final Integer _level)
            throws EFapsException
        {
            final String ret;
            if (getProdClasslist() == null) {
                ret = "-";
            } else {
                final ClassSelect classSel =  new ClassSelect() {
                    @Override
                    protected int getLevel()
                    {
                        final int ret;
                        if (_level == null) {
                            ret = super.getLevel();
                        } else {
                            ret = _level;
                        }
                        return ret;
                    }
                };
                ret = (String) classSel.evalValue(getProdClasslist());
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #prodClasslist}.
         *
         * @return value of instance variable {@link #prodClasslist}
         */
        public List<Classification> getProdClasslist()
        {
            initialize();
            return prodClasslist;
        }

        /**
         * Setter method for instance variable {@link #prodClasslist}.
         *
         * @param _prodClasslist value for instance variable
         *            {@link #prodClasslist}
         */
        public void setProdClasslist(final List<Classification> _prodClasslist)
        {
            prodClasslist = _prodClasslist;
        }

        /**
         * Getter method for the instance variable {@link #storageInstance}.
         *
         * @return value of instance variable {@link #storageInstance}
         */
        public Instance getStorageInstance()
        {
            return storageInstance;
        }

        /**
         * Setter method for instance variable {@link #storageInstance}.
         *
         * @param _storageInstance value for instance variable
         *            {@link #storageInstance}
         */
        public void setStorageInstance(final Instance _storageInstance)
        {
            storageInstance = _storageInstance;
        }
    }
}
