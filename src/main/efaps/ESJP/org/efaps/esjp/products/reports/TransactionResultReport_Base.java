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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.collections4.iterators.ReverseListIterator;
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
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

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

    public enum StorageDisplay
    {
        NONE, COLUMN, ROW;
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

    public static class DynTransactionResultReport
        extends AbstractDynamicReport
    {

        private final FilteredReport filteredReport;

        /**
         * @param _transactionResultReport_Base
         */
        public DynTransactionResultReport(final TransactionResultReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> beans = new ArrayList<>();
            final Instance prodInst = _parameter.getInstance();
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
            add2QueryBuilder(_parameter, queryBldr);
            queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, prodInst);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDoc = SelectBuilder.get().linkto(CIProducts.TransactionInOutAbstract.Document);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CIERP.DocumentAbstract.Name);
            final SelectBuilder selDocContactName = new SelectBuilder(selDoc).linkto(CIERP.DocumentAbstract.Contact)
                                .attribute(CIContacts.ContactAbstract.Name);
            multi.addSelect(selDocInst, selDocName, selDocContactName);
            multi.addAttribute(CIProducts.TransactionInOutAbstract.Quantity,
                            CIProducts.TransactionInOutAbstract.Description,
                            CIProducts.TransactionInOutAbstract.Date,
                            CIProducts.TransactionInOutAbstract.Position);
            multi.execute();
            while (multi.next()) {
                final DataBean bean = new DataBean()
                    .setTransInst(multi.getCurrentInstance())
                    .setDate(multi.<DateTime>getAttribute(CIProducts.TransactionInOutAbstract.Date))
                    .setPosition(multi.<Integer>getAttribute(CIProducts.TransactionInOutAbstract.Position))
                    .setQuantity(multi.<BigDecimal>getAttribute(CIProducts.TransactionInOutAbstract.Quantity))
                    .setDescription(multi.<String>getAttribute(CIProducts.TransactionInOutAbstract.Description))
                    .setDocInst(multi.<Instance>getSelect(selDocInst))
                    .setDocName(multi.<String>getSelect(selDocName))
                    .setDocContactName(multi.<String>getSelect(selDocContactName));
                beans.add(bean);
            }
            final ComparatorChain<DataBean> chain = new ComparatorChain<DataBean>();
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
            BigDecimal current = getInventory(_parameter, prodInst);
            getReport().addParameter("Inventory", current);
            while (iter.hasNext()) {
               final DataBean bean = iter.next();
               bean.setTotal(current);
               current = current.subtract(bean.getQuantity());
            }

            return new JRBeanCollectionDataSource(beans);
        }

        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
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

        protected BigDecimal getInventory(final Parameter _parameter,
                                          final Instance _prodInst)
            throws EFapsException
        {
            BigDecimal ret = BigDecimal.ZERO;
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
            };
            final List<? extends InventoryBean> beans = inventory.getInventory(_parameter);
            for (final InventoryBean bean : beans) {
                ret = ret.add(bean.getQuantity());
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<DateTime> dateColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Date"),
                            "date", DateTimeDate.get());

            final TextColumnBuilder<BigDecimal> inColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Incoming"),
                            "incoming", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> outColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Outgoing"),
                            "outgoing", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> descrColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.Description"),
                            "description", DynamicReports.type.stringType());

            final TextColumnBuilder<String> docNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocName"),
                            "docName", DynamicReports.type.stringType());

            final TextColumnBuilder<String> docTypeColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocType"),
                            "docType", DynamicReports.type.stringType());

            final TextColumnBuilder<String> docContactNameColumn = DynamicReports.col.column(
                            this.filteredReport.getDBProperty("Column.DocContactName"),
                            "docContactName", DynamicReports.type.stringType());

            final AggregationSubtotalBuilder<BigDecimal> incomingSum = DynamicReports.sbt.sum("incoming",
                            BigDecimal.class, inColumn);
            final AggregationSubtotalBuilder<BigDecimal> outgoingSum = DynamicReports.sbt.sum("outgoing",
                            BigDecimal.class, outColumn);
            final AggregationSubtotalBuilder<Object> totalSum = DynamicReports.sbt.aggregate(new TotalExpression(),
                            totalColumn, Calculation.NOTHING).setDataType(DynamicReports.type.bigDecimalType());
            _builder.addColumn(dateColumn, inColumn, outColumn, totalColumn, descrColumn, docTypeColumn, docNameColumn,
                             docContactNameColumn)
                            .addSubtotalAtColumnFooter(incomingSum, outgoingSum, totalSum);
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
    }

    public static class DataBean
    {

        private Instance transInst;

        private BigDecimal quantity;

        private BigDecimal total;

        private String description;

        private DateTime date;

        private Integer position;

        private Instance docInst;

        private String docName;

        private String docContactName;

        public boolean isTransOut()
        {
            return getTransInst().getType().isCIType(CIProducts.TransactionOutbound);
        }

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
         */
        public DataBean setDocContactName(final String _docContactName)
        {
            this.docContactName = _docContactName;
            return this;
        }
    }

    public static class TotalExpression
        extends AbstractSimpleExpression<BigDecimal>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            return _reportParameters.getValue("Inventory");
        }
    }
}
