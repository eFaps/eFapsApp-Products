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

package org.efaps.esjp.products.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.efaps.admin.datamodel.Status;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("7b2ad676-0bc4-4f09-a339-75808c2febb1")
@EFapsApplication("eFapsApp-Products")
public abstract class TransactionResultReport_Base
    extends FilteredReport
{

    /**
     * The Enum StorageDisplay.
     */
    public enum StorageDisplay
    {

        /** The none. */
        NONE,

        /** The group. */
        GROUP,

        /** The row. */
        ROW;
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
        dyRp.setFileName(getDBProperty("FileName"));
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
        return new DynTransactionResultReport(this);
    }

    /**
     * The Class DynTransactionResultReport.
     */
    public static class DynTransactionResultReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final FilteredReport filteredReport;

        /** The storage group. */
        private ColumnGroupBuilder storageGroup  = null;

        /** The individual. */
        private Boolean individual = null;

        /**
         * Instantiates a new dyn transaction result report.
         *
         * @param _filteredReport the _filtered report
         */
        public DynTransactionResultReport(final TransactionResultReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {
                final List<DataBean> beans = new ArrayList<>();
                final Instance prodInst = _parameter.getInstance();
                final QueryBuilder queryBldr;
                if (isIndividual(_parameter)) {
                    queryBldr = new QueryBuilder(CIProducts.TransactionIndividualAbstract);
                } else {
                    queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                }
                add2QueryBuilder(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDoc = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Document);
                final SelectBuilder selDocStatus = new SelectBuilder(selDoc).status();
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CIERP.DocumentAbstract.Name);
                final SelectBuilder selDocContactName = new SelectBuilder(selDoc).linkto(CIERP.DocumentAbstract.Contact)
                                .attribute(CIContacts.ContactAbstract.Name);
                final SelectBuilder selStorage = SelectBuilder.get().linkto(
                                CIProducts.TransactionInOutAbstract.Storage);
                final SelectBuilder selStorageInst = new SelectBuilder(selStorage).instance();
                final SelectBuilder selStorageName = new SelectBuilder(selStorage).attribute(
                                CIProducts.StorageAbstract.Name);
                final SelectBuilder selProdName = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product)
                                .attribute(CIProducts.ProductAbstract.Name);
                multi.addSelect(selDocInst, selDocStatus, selDocName, selDocContactName, selStorageName, selStorageInst,
                                selProdName);
                multi.addAttribute(CIProducts.TransactionAbstract.Quantity, CIProducts.TransactionAbstract.Description,
                                CIProducts.TransactionAbstract.Date, CIProducts.TransactionAbstract.Position);
                multi.execute();
                while (multi.next()) {
                    if (isValidStatus(_parameter, multi.<Status>getSelect(selDocStatus))) {
                        final DataBean bean = getDataBean()
                                .setTransInst(multi.getCurrentInstance())
                                .setDate(multi.<DateTime>getAttribute(CIProducts.TransactionAbstract.Date))
                                .setPosition(multi.<Integer>getAttribute(CIProducts.TransactionAbstract.Position))
                                .setQuantity(multi.<BigDecimal>getAttribute(CIProducts.TransactionAbstract.Quantity))
                                .setDescription(multi.<String>getAttribute(CIProducts.TransactionAbstract.Description))
                                .setDocInst(multi.<Instance>getSelect(selDocInst))
                                .setDocName(multi.<String>getSelect(selDocName))
                                .setDocContactName(multi.<String>getSelect(selDocContactName))
                                .setStorageName(multi.<String>getSelect(selStorageName))
                                .setStorageInst(multi.<Instance>getSelect(selStorageInst))
                                .setProdName(multi.<String>getSelect(selProdName));
                        beans.add(bean);
                        add2Bean(_parameter, bean);
                    }
                }
                final ComparatorChain<DataBean> chain = new ComparatorChain<DataBean>();
                if (StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                    chain.addComparator(new Comparator<DataBean>()
                    {

                        @Override
                        public int compare(final DataBean _bean0,
                                           final DataBean _bean1)
                        {
                            return _bean0.getStorageName().compareTo(_bean1.getStorageName());
                        }
                    });
                }
                chain.addComparator(new Comparator<DataBean>()
                {

                    @Override
                    public int compare(final DataBean _bean0,
                                       final DataBean _bean1)
                    {
                        return _bean0.getDate().compareTo(_bean1.getDate());
                    }
                });
                chain.addComparator(new Comparator<DataBean>()
                {

                    @Override
                    public int compare(final DataBean _bean0,
                                       final DataBean _bean1)
                    {
                        return _bean0.getPosition().compareTo(_bean1.getPosition());
                    }
                });

                Collections.sort(beans, chain);

                final ReverseListIterator<DataBean> iter = new ReverseListIterator<>(beans);

                final Map<Instance, BigDecimal> inventorymap = getInventory(_parameter, prodInst);
                BigDecimal inventory = BigDecimal.ZERO;
                for (final BigDecimal inven : inventorymap.values()) {
                    inventory = inventory.add(inven);
                }
                if (!beans.isEmpty()) {
                    final DataBean bean = beans.iterator().next();
                    bean.setInventoryMap(inventorymap);
                    bean.setInventory(inventory);
                }

                BigDecimal current = null;
                if (!StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                    current = inventory;
                }
                Instance currentStorageInst = null;
                while (iter.hasNext()) {
                    final DataBean bean = iter.next();
                    if (StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                        if (!bean.getStorageInst().equals(currentStorageInst)) {
                            current = inventorymap.get(bean.getStorageInst());
                            currentStorageInst = bean.getStorageInst();
                            if (current == null) {
                                current = BigDecimal.ZERO;
                            }
                        }
                    }
                    bean.setTotal(current);
                    current = current.subtract(bean.getQuantity());
                }
                ret = new JRBeanCollectionDataSource(beans);
                getFilteredReport().cache(_parameter, ret);
            }
            final Collection<?> dataCollection = ((JRBeanCollectionDataSource) ret).getData();
            if (dataCollection.isEmpty()) {
                getReport().addParameter("Inventory", BigDecimal.ZERO);
                getReport().addParameter("InventoryMap", new HashMap<Instance, BigDecimal>());
            } else {
                final DataBean bean = (DataBean) dataCollection.iterator().next();
                getReport().addParameter("Inventory", bean.getInventory());
                getReport().addParameter("InventoryMap", bean.getInventoryMap());
            }
            return ret;
        }

        /**
         * Checks if is individual.
         *
         * @param _parameter the _parameter
         * @return true, if is individual
         * @throws EFapsException on error
         */
        protected boolean isIndividual(final Parameter _parameter)
            throws EFapsException
        {
            if (this.individual == null) {
                final Parameter parameter = ParameterUtil.clone(_parameter);
                ParameterUtil.setProperty(parameter, "Individual01", "BATCH");
                ParameterUtil.setProperty(parameter, "Individual02", "INDIVIDUAL");
                final Return ret = new Product().individualAccessCheck(parameter);
                if (ret.contains(ReturnValues.TRUE)) {
                    final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
                    if (filter.containsKey("individual")) {
                        this.individual = (Boolean) filter.get("individual");
                    } else {
                        this.individual = false;
                    }
                } else {
                    this.individual = false;
                }
            }
            return this.individual;
        }

        /**
         * Add2 bean.
         *
         * @param _parameter the _parameter
         * @param _bean the _bean
         * @throws EFapsException on error
         */
        protected void add2Bean(final Parameter _parameter,
                                final DataBean _bean)
            throws EFapsException
        {
            // to be used by implementations
        }

        /**
         * Gets the data bean.
         *
         * @return the data bean
         */
        protected DataBean getDataBean()
        {
            return new DataBean();
        }

        /**
         * Checks if is valid status.
         *
         * @param _parameter the _parameter
         * @param _status the _status
         * @return true, if is valid status
         */
        protected boolean isValidStatus(final Parameter _parameter,
                                        final Status _status)
        {
            boolean ret = true;
            if (_status != null) {
                ret = !"Canceled".equals(_status.getKey());
            }
            return ret;
        }

        /**
         * Add2 query builder.
         *
         * @param _parameter the _parameter
         * @param _queryBldr the _query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Instance prodInst = _parameter.getInstance();
            if (isIndividual(_parameter)) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.StockProductAbstract2IndividualAbstract);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.StockProductAbstract2IndividualAbstract.FromAbstract,
                                prodInst);
                _queryBldr.addWhereAttrInQuery(CIProducts.TransactionAbstract.Product,
                                attrQueryBldr.getAttributeQuery(
                                        CIProducts.StockProductAbstract2IndividualAbstract.ToAbstract));
            } else {
                _queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, prodInst);
            }
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filter.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CIProducts.TransactionAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }

            final List<Instance> storageInst = getStorageInsts(_parameter);
            if (!storageInst.isEmpty()) {
                _queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Storage, storageInst.toArray());
            }
        }

        /**
         * Gets the storage insts.
         *
         * @param _parameter the _parameter
         * @return the storage insts
         * @throws EFapsException on error
         */
        protected List<Instance> getStorageInsts(final Parameter _parameter)
            throws EFapsException
        {
            final List<Instance> ret = new ArrayList<>();
            final Map<String, Object> map = getFilteredReport().getFilterMap(_parameter);
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
         * Gets the inventory.
         *
         * @param _parameter the _parameter
         * @param _prodInst the _prod inst
         * @return the inventory
         * @throws EFapsException on error
         */
        protected Map<Instance, BigDecimal> getInventory(final Parameter _parameter,
                                                         final Instance _prodInst)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);

            final Map<Instance, BigDecimal> ret = new HashMap<>();
            final Inventory inventory = new Inventory()
            {
                @Override
                protected void add2QueryBuilder(final Parameter _parameter,
                                                final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder(_parameter, _queryBldr);
                    _queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, _prodInst);
                }

                @Override
                protected void add2QueryBuilder4Transaction(final Parameter _parameter,
                                                            final QueryBuilder _queryBldr)
                    throws EFapsException
                {
                    super.add2QueryBuilder4Transaction(_parameter, _queryBldr);
                    _queryBldr.addWhereAttrEqValue(CIProducts.TransactionAbstract.Product, _prodInst);
                }
            };
            inventory.setShowStorage(true);
            if (filter.containsKey("dateTo")) {
                final DateTime date = (DateTime) filter.get("dateTo");
                inventory.setDate(date.withTimeAtStartOfDay().plusDays(1));
            }
            final List<? extends InventoryBean> beans = inventory.getInventory(_parameter);
            for (final InventoryBean bean : beans) {
                if (!ret.containsKey(bean.getStorageInstance())) {
                    ret.put(bean.getStorageInstance(), BigDecimal.ZERO);
                }
                ret.put(bean.getStorageInstance(), ret.get(bean.getStorageInstance()).add(bean.getQuantity()));
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            if (!StorageDisplay.NONE.equals(getStorageDisplay(_parameter))) {
                final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(
                                this.filteredReport.getDBProperty("Column.StorageName"),
                                "storageName", DynamicReports.type.stringType());
                _builder.addColumn(storageColumn);
                if (StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                    this.storageGroup = DynamicReports.grp.group(storageColumn);
                    _builder.groupBy(this.storageGroup);
                }
            }

            final TextColumnBuilder<DateTime> dateColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Date"),
                            "date", DateTimeDate.get());
            _builder.addColumn(dateColumn);

            if (isIndividual(_parameter)) {
                final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(
                                this.filteredReport.getDBProperty("Column.ProdName"),
                                "prodName", DynamicReports.type.stringType());
                _builder.addColumn(prodNameColumn);
            }

            final TextColumnBuilder<BigDecimal> inColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Incoming"),
                            "incoming", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> outColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Outgoing"),
                            "outgoing", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> docContactNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocContactName"),
                            "docContactName", DynamicReports.type.stringType()).setWidth(200);

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocName"),
                            "docName", DynamicReports.type.stringType());

            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocType"),
                            "docType", DynamicReports.type.stringType());

            final TextColumnBuilder<String> descrColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Description"),
                            "description", DynamicReports.type.stringType()).setWidth(200);

            final AggregationSubtotalBuilder<BigDecimal> incomingSum = DynamicReports.sbt.sum("incoming",
                            BigDecimal.class, inColumn);
            final AggregationSubtotalBuilder<BigDecimal> outgoingSum = DynamicReports.sbt.sum("outgoing",
                            BigDecimal.class, outColumn);
            final AggregationSubtotalBuilder<Object> totalSum = DynamicReports.sbt.aggregate(new TotalExpression(false),
                            totalColumn, Calculation.NOTHING).setDataType(DynamicReports.type.bigDecimalType());
            _builder.addColumn(inColumn, outColumn, totalColumn, docContactNameColumn, docTypeColumn,
                            docNameColumn, descrColumn)
                            .addSubtotalAtSummary(incomingSum, outgoingSum, totalSum);

            if (this.storageGroup != null) {
                final AggregationSubtotalBuilder<BigDecimal> incomingSum4Grp = DynamicReports.sbt.sum("incoming",
                                BigDecimal.class, inColumn);
                final AggregationSubtotalBuilder<BigDecimal> outgoingSum4Grp = DynamicReports.sbt.sum("outgoing",
                                BigDecimal.class, outColumn);
                final AggregationSubtotalBuilder<Object> totalSum4Grp = DynamicReports.sbt.aggregate(
                                new TotalExpression(true),
                                totalColumn, Calculation.NOTHING).setDataType(DynamicReports.type.bigDecimalType());
                _builder.subtotalsAtGroupFooter(this.storageGroup, incomingSum4Grp, outgoingSum4Grp, totalSum4Grp);
            }
            _builder.addField("storageInst", Instance.class);
        }

        /**
         * Gets the storage display.
         *
         * @param _parameter the _parameter
         * @return the storage display
         * @throws EFapsException on error
         */
        protected StorageDisplay getStorageDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilteredReport().getFilterMap(_parameter).get(
                            "storageDisplay");
            StorageDisplay ret;
            if (filter != null) {
                ret = (StorageDisplay) filter.getObject();
            } else {
                ret = StorageDisplay.NONE;
            }
            return ret;
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
         * Getter method for the instance variable {@link #storageGroup}.
         *
         * @return value of instance variable {@link #storageGroup}
         */
        protected ColumnGroupBuilder getStorageGroup()
        {
            return this.storageGroup;
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The trans inst. */
        private Instance transInst;

        /** The quantity. */
        private BigDecimal quantity;

        /** The total. */
        private BigDecimal total;

        /** The description. */
        private String description;

        /** The date. */
        private DateTime date;

        /** The position. */
        private Integer position;

        /** The doc inst. */
        private Instance docInst;

        /** The doc name. */
        private String docName;

        /** The doc contact name. */
        private String docContactName;

        /** The storage name. */
        private String storageName;

        /** The storage inst. */
        private Instance storageInst;

        /** The prod name. */
        private String prodName;

        // this values will only be used in the first bean
        /** The inventoryMap. */
        private Map<Instance, BigDecimal> inventoryMap;


        /** The inventory. */
        private BigDecimal inventory;

        /**
         * Checks if is trans out.
         *
         * @return true, if is trans out
         */
        public boolean isTransOut()
        {
            return getTransInst().getType().isCIType(CIProducts.TransactionOutbound)
                            || getTransInst().getType().isCIType(CIProducts.TransactionIndividualOutbound);
        }

        /**
         * Gets the doc type.
         *
         * @return the doc type
         */
        public String getDocType()
        {
            String ret;
            if (getDocInst() != null && getDocInst().isValid()) {
                ret = getDocInst().getType().getLabel();
            } else {
                ret = null;
            }
            return ret;
        }

        /**
         * Gets the outgoing.
         *
         * @return the outgoing
         */
        public BigDecimal getOutgoing()
        {
            BigDecimal ret;
            if (isTransOut()) {
                ret = this.quantity;
            } else {
                ret = null;
            }
            return ret;
        }

        /**
         * Gets the incoming.
         *
         * @return the incoming
         */
        public BigDecimal getIncoming()
        {
            BigDecimal ret;
            if (isTransOut()) {
                ret = null;
            } else {
                ret = this.quantity;
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            BigDecimal ret = this.quantity;
            if (isTransOut()) {
                ret = this.quantity.negate();
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #quantity}.
         *
         * @param _quantity value for instance variable {@link #quantity}
         * @return the data bean
         */
        public DataBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #description}.
         *
         * @return value of instance variable {@link #description}
         */
        public String getDescription()
        {
            return this.description;
        }

        /**
         * Setter method for instance variable {@link #description}.
         *
         * @param _description value for instance variable {@link #description}
         * @return the data bean
         */
        public DataBean setDescription(final String _description)
        {
            this.description = _description;
            return this;
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
         * Getter method for the instance variable {@link #transInst}.
         *
         * @return value of instance variable {@link #transInst}
         */
        public Instance getTransInst()
        {
            return this.transInst;
        }

        /**
         * Setter method for instance variable {@link #transInst}.
         *
         * @param _transInst value for instance variable {@link #transInst}
         * @return the data bean
         */
        public DataBean setTransInst(final Instance _transInst)
        {
            this.transInst = _transInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #total}.
         *
         * @return value of instance variable {@link #total}
         */
        public BigDecimal getTotal()
        {
            return this.total;
        }

        /**
         * Setter method for instance variable {@link #total}.
         *
         * @param _total value for instance variable {@link #total}
         * @return the data bean
         */
        public DataBean setTotal(final BigDecimal _total)
        {
            this.total = _total;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #position}.
         *
         * @return value of instance variable {@link #position}
         */
        public Integer getPosition()
        {
            return this.position;
        }

        /**
         * Setter method for instance variable {@link #position}.
         *
         * @param _position value for instance variable {@link #position}
         * @return the data bean
         */
        public DataBean setPosition(final Integer _position)
        {
            this.position = _position;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return the data bean
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return the data bean
         */
        public DataBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docContactName}.
         *
         * @return value of instance variable {@link #docContactName}
         */
        public String getDocContactName()
        {
            return this.docContactName;
        }

        /**
         * Setter method for instance variable {@link #docContactName}.
         *
         * @param _docContactName value for instance variable {@link #docContactName}
         * @return the data bean
         */
        public DataBean setDocContactName(final String _docContactName)
        {
            this.docContactName = _docContactName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #storage}.
         *
         * @return value of instance variable {@link #storage}
         */
        public String getStorageName()
        {
            return this.storageName;
        }

        /**
         * Setter method for instance variable {@link #storage}.
         *
         * @param _storage value for instance variable {@link #storage}
         * @return the data bean
         */
        public DataBean setStorageName(final String _storage)
        {
            this.storageName = _storage;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #storageInst}.
         *
         * @return value of instance variable {@link #storageInst}
         */
        public Instance getStorageInst()
        {
            return this.storageInst;
        }

        /**
         * Setter method for instance variable {@link #storageInst}.
         *
         * @param _storageInst value for instance variable {@link #storageInst}
         * @return the data bean
         */
        public DataBean setStorageInst(final Instance _storageInst)
        {
            this.storageInst = _storageInst;
            return this;
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
         * @return the data bean
         */
        public DataBean setProdName(final String _prodName)
        {
            this.prodName = _prodName;
            return this;
        }

        /**
         * Gets the inventorymap.
         *
         * @return the inventorymap
         */
        public Map<Instance, BigDecimal> getInventoryMap()
        {
            return this.inventoryMap;
        }

        /**
         * Sets the inventorymap.
         *
         * @param _inventoryMap the _inventory map
         */
        public void setInventoryMap(final Map<Instance, BigDecimal> _inventoryMap)
        {
            this.inventoryMap = _inventoryMap;
        }

        /**
         * Gets the inventory.
         *
         * @return the inventory
         */
        public BigDecimal getInventory()
        {
            return this.inventory;
        }

        /**
         * Sets the inventory.
         *
         * @param _inventory the new inventory
         */
        public void setInventory(final BigDecimal _inventory)
        {
            this.inventory = _inventory;
        }
    }

    /**
     * The Class TotalExpression.
     */
    public static class TotalExpression
        extends AbstractSimpleExpression<BigDecimal>
    {
        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /** The group. */
        private final boolean group;

        /**
         * Instantiates a new total expression.
         *
         * @param _group the _group
         */
        public TotalExpression(final boolean _group)
        {
            this.group = _group;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            BigDecimal ret;
            if (this.group) {
                final Map<Instance, BigDecimal> inventorymap = _reportParameters.getValue("InventoryMap");
                final Object storageInst = _reportParameters.getFieldValue("storageInst");
                ret = inventorymap.get(storageInst);
            } else {
                ret = _reportParameters.getValue("Inventory");
            }
            return ret;
        }
    }
}
