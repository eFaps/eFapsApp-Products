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

package org.efaps.esjp.products.reports;

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.esjp.products.util.Products;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.Days;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.style.ConditionalStyleBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("45d70db0-b427-461f-8d34-f4872c1708a4")
@EFapsApplication("eFapsApp-Products")
public abstract class LastMovementReport_Base
    extends FilteredReport
{

    /**
     * The Enum StorageDisplay.
     *
     */
    public enum StorageDisplay
    {

        /** The none. */
        NONE,

        /** The column. */
        COLUMN,

        /** The group. */
        GROUP;
    }

    /**
     * The Enum TypeDisplay.
     */
    public enum TypeDisplay
    {

        /** The none. */
        NONE,

        /** The column. */
        COLUMN,

        /** The group. */
        GROUP;
    }

    /**
     * The Enum ClassDisplay.
     */
    public enum ClassDisplay
    {

        /** The none. */
        NONE,

        /** The column. */
        COLUMN,

        /** The group. */
        GROUP;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(LastMovementReport.class.getName() + ".FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynLastMovementReport(this);
    }

    /**
     * Report class.
     */
    public static class DynLastMovementReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final FilteredReport filteredReport;

        /** The beans. */
        private List<DataBean> beans;

        /**
         * Instantiates a new dyn last movement report.
         *
         * @param _filteredReport the filtered report
         */
        public DynLastMovementReport(final LastMovementReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (this.filteredReport.isCached(_parameter)) {
                ret = this.filteredReport.getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    LOG.error("Catched error", e);
                }
            } else {
                final DateTime date = (DateTime) getFilterMap(_parameter).get("date");
                final DateTime mindate = date.minusDays(getMaxDays(_parameter));
                final List<DataBean> tmpBeans = getBeans(_parameter, null);
                final Map<Instance, List<DataBean>> prod2beans = new HashMap<>();
                for (final DataBean bean : tmpBeans) {
                    List<DataBean> ilist;
                    if (prod2beans.containsKey(bean.getProdInstance())) {
                        ilist = prod2beans.get(bean.getProdInstance());
                    } else {
                        ilist = new ArrayList<>();
                        prod2beans.put(bean.getProdInstance(), ilist);
                    }
                    ilist.add(bean);
                }
                final Set<Instance> done = new HashSet<>();
                Collection<Instance> prodFilter = prod2beans.keySet();
                DateTime startDate = date;
                while (!prodFilter.isEmpty() && mindate.isBefore(startDate)) {
                    final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInbound);
                    queryBldr.addType(CIProducts.TransactionOutbound);
                    if (includeIndividual(_parameter)) {
                        queryBldr.addType(CIProducts.TransactionIndividualInbound,
                                        CIProducts.TransactionIndividualOutbound);
                    }
                    add2QueryBldr(_parameter, queryBldr);
                    queryBldr.addWhereAttrLessValue(CIProducts.TransactionAbstract.Date, startDate.plusMinutes(1));
                    queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date,
                                    startDate.minusDays(10).minusMinutes(1));
                    queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, prodFilter.toArray());
                    queryBldr.addOrderByAttributeDesc(CIProducts.TransactionAbstract.Date);
                    final MultiPrintQuery multi = queryBldr.getPrint();
                    multi.setEnforceSorted(true);
                    final SelectBuilder selProdInst = SelectBuilder.get().linkto(
                                    CIProducts.TransactionAbstract.Product).instance();
                    multi.addSelect(selProdInst);
                    multi.addAttribute(CIProducts.TransactionAbstract.Date,
                                    CIProducts.TransactionAbstract.Quantity);
                    multi.execute();
                    while (multi.next() && done.size() < tmpBeans.size()) {
                        final Instance prodInst = multi.getSelect(selProdInst);
                        if (!done.contains(prodInst)) {
                            final List<DataBean> beanList = prod2beans.get(prodInst);
                            final DateTime transDate = multi.getAttribute(CIProducts.TransactionAbstract.Date);
                            final BigDecimal quantity = multi.getAttribute(CIProducts.TransactionAbstract.Quantity);

                            if (validateThreshold(_parameter, multi.getCurrentInstance(), quantity)) {
                                final Iterator<DataBean> beanIter = beanList.iterator();
                                final DataBean bean = beanIter.next();
                                bean.setDate(date);
                                if (bean.addMovement(multi.getCurrentInstance(), transDate, quantity)) {
                                    done.add(prodInst);
                                }
                                while (beanIter.hasNext()) {
                                    beanIter.next().setLastOutDate(bean.getLastOutDate()).setDate(bean.getDate())
                                        .setLastOutAmount(quantity);
                                }
                            }
                        }
                    }
                    prodFilter = CollectionUtils.disjunction(prod2beans.keySet(), done);
                    startDate = startDate.minusDays(10);
                }
                final ComparatorChain<DataBean> chain = new ComparatorChain<DataBean>();
                if (!StorageDisplay.NONE.equals(getStorageDisplay(_parameter))) {
                    chain.addComparator(new Comparator<DataBean>()
                    {
                        @Override
                        public int compare(final DataBean _bean0,
                                           final DataBean _bean1)
                        {
                            return _bean0.getStorage().compareTo(_bean1.getStorage());
                        }
                    });
                }
                if (!TypeDisplay.NONE.equals(getTypeDisplay(_parameter))) {
                    chain.addComparator(new Comparator<DataBean>()
                    {
                        @Override
                        public int compare(final DataBean _bean0,
                                           final DataBean _bean1)
                        {
                            return _bean0.getProdType().compareTo(_bean1.getProdType());
                        }
                    });
                }
                chain.addComparator(new Comparator<DataBean>()
                {
                    @Override
                    public int compare(final DataBean _bean0,
                                       final DataBean _bean1)
                    {
                        return _bean0.getProdName().compareTo(_bean1.getProdName());
                    }
                });
                Collections.sort(tmpBeans, chain);

                ret =  new JRBeanCollectionDataSource(tmpBeans);
                this.filteredReport.cache(_parameter, ret);
            }
            return ret;
        }


        /**
         * Validate threshold.
         *
         * @param _parameter the _parameter
         * @param _transactionInstance the _transaction instance
         * @param _quantity the _quantity
         * @return true, if successful
         * @throws EFapsException the e faps exception
         */
        protected boolean validateThreshold(final Parameter _parameter,
                                            final Instance _transactionInstance,
                                            final BigDecimal _quantity)
                                                throws EFapsException
        {
            return (_transactionInstance.getType().isCIType(CIProducts.TransactionOutbound)
                            || _transactionInstance.getType().isCIType(CIProducts.TransactionIndividualOutbound))
                            && _quantity.compareTo(new BigDecimal(getOutThreshold(_parameter))) > 0
                            ||
                    (_transactionInstance.getType().isCIType(CIProducts.TransactionInbound)
                            || _transactionInstance.getType().isCIType(CIProducts.TransactionIndividualInbound))
                                && _quantity.compareTo(new BigDecimal(getInThreshold(_parameter))) > 0;
        }

        /**
         * Gets the storage display.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the storage display
         * @throws EFapsException on error
         */
        protected StorageDisplay getStorageDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("storageDisplay");
            StorageDisplay ret;
            if (filter != null) {
                ret = (StorageDisplay) filter.getObject();
            } else {
                ret = StorageDisplay.NONE;
            }
            return ret;
        }

        /**
         * Gets the beans.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _inventory the inventory
         * @return the beans
         * @throws EFapsException on error
         */
        @SuppressWarnings("unchecked")
        public List<DataBean> getBeans(final Parameter _parameter,
                                       final Inventory _inventory)
            throws EFapsException
        {
            if (this.beans == null) {
                final Inventory inventory;
                if (_inventory == null) {
                    inventory = getInventoryObject(_parameter);
                    inventory.setStorageInsts(getStorageInsts(_parameter));
                    inventory.setCurrencyInst(getCurrencyInst(_parameter));
                    inventory.setShowStorage(!StorageDisplay.NONE.equals(getStorageDisplay(_parameter)));
                    inventory.setShowProdClass(!ClassDisplay.NONE.equals(getClassDisplay(_parameter)));
                    inventory.setDate((DateTime) getFilterMap(_parameter).get("date"));
                } else {
                    inventory = _inventory;
                }
                this.beans = (List<DataBean>) inventory.getInventory(_parameter);
            }
            return this.beans;
        }

        /**
         * Add to query bldr.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr the query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            // only transactions belonging to official Documents
            final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, Products.REPLASTMOVE.get());
            if (queryBldr != null) {
                _queryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Document,
                                queryBldr.getAttributeQuery(CIERP.DocumentAbstract.ID));
            }
        }

        /**
         * Gets the filter map.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the filter map
         * @throws EFapsException on error
         */
        protected Map<String, Object> getFilterMap(final Parameter _parameter)
            throws EFapsException
        {
            return getFilteredReport().getFilterMap(_parameter);
        }

        /**
         * Gets the storage insts.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the storage insts
         * @throws EFapsException on error
         */
        protected List<Instance> getStorageInsts(final Parameter _parameter)
            throws EFapsException
        {
            final List<Instance> ret = new ArrayList<>();
            final Map<String, Object> map = getFilterMap(_parameter);
            if (map.containsKey("storageGroup")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storageGroup");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.addAll(new StorageGroup().getStorage4Group(_parameter, filter.getObject()));
                }
            }

            if (ret.isEmpty() && map.containsKey("storage")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("storage");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.add(filter.getObject());
                }
            }
            return ret;
        }

        /**
         * Gets the currency inst.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the currency inst
         * @throws EFapsException on error
         */
        protected Instance getCurrencyInst(final Parameter _parameter)
            throws EFapsException
        {
            Instance ret = null;
            if ("true".equalsIgnoreCase(getProperty(_parameter, "EvaluateCost"))) {
                final Map<String, Object> map = getFilterMap(_parameter);
                if (map.containsKey("currency")) {
                    final CurrencyFilterValue filter = (CurrencyFilterValue) map.get("currency");
                    ret = filter.getObject();
                }
            }
            return ret;
        }

        /**
         * Gets the type display.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the type display
         * @throws EFapsException on error
         */
        protected TypeDisplay getTypeDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("typeDisplay");
            TypeDisplay ret;
            if (filter != null) {
                ret = (TypeDisplay) filter.getObject();
            } else {
                ret = TypeDisplay.NONE;
            }
            return ret;
        }

        /**
         * Gets the class display.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the class display
         * @throws EFapsException on error
         */
        protected ClassDisplay getClassDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("classDisplay");
            ClassDisplay ret;
            if (filter != null) {
                ret = (ClassDisplay) filter.getObject();
            } else {
                ret = ClassDisplay.NONE;
            }
            return ret;
        }

        /**
         * Adds the conditional style.
         *
         * @param _styleBldr the style bldr
         * @param _key the _key
         * @throws EFapsException on error
         */
        protected void addConditionalStyle(final StyleBuilder _styleBldr,
                                           final String _key)
            throws EFapsException
        {
            final Properties properties = Products.REPLASTMOVE.get();
            int j = 1;
            while (properties.containsKey(_key + "Color" + String.format("%02d", j))) {
                final ConditionExpression condition = new ConditionExpression(_key);
                final ConditionalStyleBuilder conditionStyle = DynamicReports.stl.conditionalStyle(condition);
                conditionStyle.setForegroundColor(Color.decode(properties.getProperty(_key + "Color"
                                + String.format("%02d", j))));
                if (properties.containsKey(_key + "Greater" + String.format("%02d", j))) {
                    condition.setGreater(properties.getProperty(_key + "Greater" + String.format("%02d", j)));
                }
                if (properties.containsKey(_key + "Smaller" + String.format("%02d", j))) {
                    condition.setSmaller(properties.getProperty(_key + "Smaller" + String.format("%02d", j)));
                }
                _styleBldr.addConditionalStyle(conditionStyle);
                j++;
            }
        }

        /**
         * Gets the in threshold.
         *
         * @param _parameter the _parameter
         * @return the in threshold
         * @throws EFapsException the e faps exception
         */
        protected Integer getInThreshold(final Parameter _parameter)
            throws EFapsException
        {
            return Integer.parseInt(Products.REPLASTMOVE.get().getProperty("inThreshold", "0"));
        }

        /**
         * Gets the out threshold.
         *
         * @param _parameter the _parameter
         * @return the out threshold
         * @throws EFapsException the e faps exception
         */
        protected Integer getOutThreshold(final Parameter _parameter)
            throws EFapsException
        {
            return Integer.parseInt(Products.REPLASTMOVE.get().getProperty("outThreshold", "0"));
        }

        /**
         * Gets the max days.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the max days
         * @throws EFapsException on error
         */
        protected Integer getMaxDays(final Parameter _parameter)
            throws EFapsException
        {
            final Properties properties = Products.REPLASTMOVE.get();
            return Integer.parseInt(properties.getProperty("maxDays", "180"));
        }

        /**
         * Include individual.
         *
         * @param _parameter the _parameter
         * @return true, if successful
         * @throws EFapsException the e faps exception
         */
        protected boolean includeIndividual(final Parameter _parameter)
            throws EFapsException
        {
            return Products.ACTIVATEINDIVIDUAL.get()
                            && Boolean.parseBoolean(
                                            Products.REPLASTMOVE.get().getProperty("activateIndividual", "true"));
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public FilteredReport getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Gets the inventory object.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the inventory object
         */
        protected Inventory getInventoryObject(final Parameter _parameter)
        {
            return new Inventory()
            {
                @Override
                protected InventoryBean getBean(final Parameter _parameter)
                    throws EFapsException
                {
                    return new DataBean();
                }

                @Override
                protected Type getInventoryType(final Parameter _parameter)
                    throws EFapsException
                {
                    return includeIndividual(_parameter)
                                    ? super.getInventoryType(_parameter)
                                    : CIProducts.Inventory.getType();
                }
            };
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final List<ColumnGridComponentBuilder> grids = new ArrayList<>();

            ColumnGroupBuilder storageGroup = null;
            if (!StorageDisplay.NONE.equals(getStorageDisplay(_parameter))) {
                final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.storage"), "storage", DynamicReports.type.stringType());
                _builder.addColumn(storageColumn);
                grids.add(storageColumn);
                if (StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                    storageGroup = DynamicReports.grp.group(storageColumn);
                    _builder.groupBy(storageGroup);

                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    //
                }
            }

            final ColumnTitleGroupBuilder prodGroup = DynamicReports.grid.titleGroup(getFilteredReport().getDBProperty(
                            "TitleGroup.product"));

            ColumnGroupBuilder typeGroup = null;
            if (!TypeDisplay.NONE.equals(getTypeDisplay(_parameter))) {
                final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.prodType"), "prodType", DynamicReports.type.stringType())
                                .setWidth(150);
                _builder.addColumn(typeColumn);
                grids.add(typeColumn);
                if (TypeDisplay.GROUP.equals(getTypeDisplay(_parameter))) {
                    typeGroup = DynamicReports.grp.group(typeColumn);
                    _builder.groupBy(typeGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(typeColumn);
                }
            }
            ColumnGroupBuilder classGroup = null;
            if (!ClassDisplay.NONE.equals(getClassDisplay(_parameter))) {
                final TextColumnBuilder<String> classColumn = DynamicReports.col.column(getFilteredReport()
                                .getDBProperty("Column.prodClass"), "prodClass", DynamicReports.type.stringType())
                                .setWidth(250);
                _builder.addColumn(classColumn);
                grids.add(classColumn);
                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    classGroup = DynamicReports.grp.group(classColumn);
                    _builder.groupBy(classGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(classColumn);
                }
            }
            final ColumnTitleGroupBuilder prodTitleGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("Group.Product"));
            if (getExType().equals(ExportType.HTML)) {
                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement("http://www.efaps.org",
                                "efapslink").addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new ProductLinkExpression())
                                .setHeight(12).setWidth(25);
                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
                prodTitleGroup.add(linkColumn);
                _builder.addColumn(linkColumn);
                prodGroup.add(linkColumn);
            }

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.prodName"), "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.prodDescr"), "prodDescr", DynamicReports.type.stringType()).setWidth(
                                            200);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(getFilteredReport().getDBProperty(
                            "Column.uoM"), "uoM", DynamicReports.type.stringType());
            _builder.addColumn(prodNameColumn, prodDescrColumn, uoMColumn);

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.quantity"), "quantity", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.reserved"), "reserved", DynamicReports.type.bigDecimalType());
            _builder.addColumn(quantityColumn, reservedColumn);
            prodTitleGroup.add(prodNameColumn, prodDescrColumn, uoMColumn, quantityColumn, reservedColumn);

            final TextColumnBuilder<DateTime> lastOutDateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.lastOutDate"), "lastOutDate", DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> lastOutAmountColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.lastOutAmount"), "lastOutAmount",
                            DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<Integer> outDaysColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.outDays"), "outDays", DynamicReports.type.integerType());
            final StyleBuilder outStyleBldr = DynamicReports.stl.style().setBold(true);
            if (!getExType().equals(ExportType.PDF)) {
                outStyleBldr.setPadding(DynamicReports.stl.padding(10));
            }
            addConditionalStyle(outStyleBldr, "outDays");
            outDaysColumn.setStyle(outStyleBldr);

            final ColumnTitleGroupBuilder outGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("Group.Out"), lastOutDateColumn, lastOutAmountColumn, outDaysColumn);

            final TextColumnBuilder<DateTime> lastInDateColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.lastInDate"), "lastInDate", DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> lastInAmountColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.lastInAmount"), "lastInAmount",
                            DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<Integer> inDaysColumn = DynamicReports.col.column(getFilteredReport().getDBProperty(
                            "Column.inDays"), "inDays", DynamicReports.type.integerType());
            final StyleBuilder inStyleBldr = DynamicReports.stl.style().setBold(true);
            if (!getExType().equals(ExportType.PDF)) {
                inStyleBldr.setPadding(DynamicReports.stl.padding(10));
            }
            addConditionalStyle(inStyleBldr, "inDays");
            inDaysColumn.setStyle(inStyleBldr);

            final ColumnTitleGroupBuilder inGroup = DynamicReports.grid.titleGroup(getFilteredReport()
                            .getDBProperty("Group.In"), lastInDateColumn, lastInAmountColumn, inDaysColumn);

            _builder.addColumn(lastOutDateColumn, lastOutAmountColumn, outDaysColumn, lastInDateColumn,
                            lastInAmountColumn, inDaysColumn);

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.cost"), "cost", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.total"), "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(getFilteredReport()
                            .getDBProperty("Column.currency"), "currency", DynamicReports.type.stringType());

            if (getCurrencyInst(_parameter) != null) {
                prodTitleGroup.add(costColumn, totalColumn, currencyColumn);
                _builder.addColumn(costColumn, totalColumn, currencyColumn);
                _builder.addSubtotalAtColumnFooter(DynamicReports.sbt.sum(totalColumn));
            }
            grids.add(prodTitleGroup);
            grids.add(outGroup);
            grids.add(inGroup);
            _builder.columnGrid(grids.toArray(new ColumnGridComponentBuilder[grids.size()]));
        }
    }

    /**
     * Expression used to render a link for the UserInterface.
     */
    public static class ProductLinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Costructor.
         */
        public ProductLinkExpression()
        {
            addExpression(DynamicReports.field("prodOID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

    /**
     * The Class DataBean.
     *
     */
    public static class DataBean
        extends InventoryBean
    {

        /** The last date. */
        private DateTime lastOutDate;

        /** The last amount. */
        private BigDecimal lastOutAmount;

        /** The last date. */
        private DateTime lastInDate;

        /** The last amount. */
        private BigDecimal lastInAmount;

        /** The date. */
        private DateTime date;

        /** The check quantity. */
        private BigDecimal checkQuantity = BigDecimal.ZERO;

        /**
         * Getter method for the instance variable {@link #lastDate}.
         *
         * @return value of instance variable {@link #lastDate}
         */
        public DateTime getLastOutDate()
        {
            return this.lastOutDate;
        }

        /**
         * Adds the movement.
         *
         * @param _transInst the _transaction instance
         * @param _transDate the transaction date
         * @param _transQuantity the transaction quantity
         * @return true, if successful
         */
        public boolean addMovement(final Instance _transInst,
                                   final DateTime _transDate,
                                   final BigDecimal _transQuantity)
        {
            boolean ret = false;
            final boolean out = _transInst.getType().isCIType(CIProducts.TransactionOutbound)
                            || _transInst.getType().isCIType(CIProducts.TransactionIndividualOutbound);

            final BigDecimal transQuantity = out ? _transQuantity.negate() : _transQuantity;
            if (out && getLastOutDate() == null) {
                setLastOutDate(_transDate);
                setLastOutAmount(transQuantity);
            } else if (!out && getLastInDate() == null) {
                setLastInDate(_transDate);
                setLastInAmount(transQuantity);
            }

            // if both are set ==> validate
            if (getLastOutDate() != null && getLastInDate() != null) {
                // last movements where on the same day
                if (Days.daysBetween(getLastOutDate(), getLastInDate()).getDays() == 0) {
                    // the amounts do cancel each other
                    if (getLastOutAmount().compareTo(getLastOutAmount()) == 0) {
                        setLastOutDate(null);
                        setLastInDate(null);
                        setLastOutAmount(null);
                        setLastInAmount(null);
                    } else {
                        this.checkQuantity = this.checkQuantity.add(transQuantity);
                        if (this.checkQuantity.compareTo(BigDecimal.ZERO) == 0) {
                            setLastOutDate(null);
                            setLastInDate(null);
                        }
                    }
                } else {
                    ret = true;
                }
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #lastDate}.
         *
         * @param _lastDate value for instance variable {@link #lastDate}
         * @return the data bean
         */
        public DataBean setLastOutDate(final DateTime _lastDate)
        {
            this.lastOutDate = _lastDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #days}.
         *
         * @return value of instance variable {@link #days}
         */
        public Integer getOutDays()
        {
            Integer ret = -1;
            if (getLastOutDate() != null && getDate() != null) {
                ret = Days.daysBetween(getLastOutDate(), getDate()).getDays();
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #days}.
         *
         * @return value of instance variable {@link #days}
         */
        public Integer getInDays()
        {
            Integer ret = -1;
            if (getLastInDate() != null && getDate() != null) {
                ret = Days.daysBetween(getLastInDate(), getDate()).getDays();
            }
            return ret;
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
         * @return the data bean
         */
        public DataBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }

        /**
         * Gets the last amount.
         *
         * @return the last amount
         */
        public BigDecimal getLastOutAmount()
        {
            return this.lastOutAmount;
        }

        /**
         * Sets the last amount.
         *
         * @param _lastAmount the last amount
         * @return the data bean
         */
        public DataBean setLastOutAmount(final BigDecimal _lastAmount)
        {
            this.lastOutAmount = _lastAmount;
            return this;
        }

        /**
         * Gets the last date.
         *
         * @return the last date
         */
        public DateTime getLastInDate()
        {
            return this.lastInDate;
        }

        /**
         * Sets the last in date.
         *
         * @param _lastInDate the last in date
         * @return the data bean
         */
        public DataBean setLastInDate(final DateTime _lastInDate)
        {
            this.lastInDate = _lastInDate;
            return this;
        }

        /**
         * Gets the last amount.
         *
         * @return the last amount
         */
        public BigDecimal getLastInAmount()
        {
            return this.lastInAmount;
        }

        /**
         * Sets the last in amount.
         *
         * @param _lastInAmount the last in amount
         * @return the data bean
         */
        public DataBean setLastInAmount(final BigDecimal _lastInAmount)
        {
            this.lastInAmount = _lastInAmount;
            return this;
        }
    }

    /**
     * The Class ConditionExpression.
     *
     */
    public static class ConditionExpression
        extends AbstractSimpleExpression<Boolean>
    {

        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The greater. */
        private String greater;

        /** The smaller. */
        private String smaller;

        /** The key. */
        private final String key;

        /**
         * Instantiates a new condition expression.
         *
         * @param _key the _key
         */
        public ConditionExpression(final String _key) {
            this.key = _key;
        }

        @Override
        public Boolean evaluate(final ReportParameters _reportParameters)
        {
            boolean ret = true;
            final Integer days = _reportParameters.getValue(this.key);

            if (getSmaller() != null) {
                ret = ret && Integer.parseInt(getSmaller()) > days;
            }
            if (getGreater() != null) {
                ret = ret && Integer.parseInt(getGreater()) < days;
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #greater}.
         *
         * @return value of instance variable {@link #greater}
         */
        public String getGreater()
        {
            return this.greater;
        }

        /**
         * Setter method for instance variable {@link #greater}.
         *
         * @param _greater value for instance variable {@link #greater}
         */
        public void setGreater(final String _greater)
        {
            this.greater = _greater;
        }

        /**
         * Getter method for the instance variable {@link #smaller}.
         *
         * @return value of instance variable {@link #smaller}
         */
        public String getSmaller()
        {
            return this.smaller;
        }

        /**
         * Setter method for instance variable {@link #smaller}.
         *
         * @param _smaller value for instance variable {@link #smaller}
         */
        public void setSmaller(final String _smaller)
        {
            this.smaller = _smaller;
        }
    }

}
