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
package org.efaps.esjp.products.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("7b27cc62-9596-4e98-b68a-545db6ee8c88")
@EFapsApplication("eFapsApp-Products")
public abstract class TransactionReport_Base
    extends FilteredReport
{

    public enum StorageDisplay
    {
        NONE, COLUMN, ROW;
    }

    public enum TypeDisplay
    {
        NONE, COLUMN, ROW;
    }

    public enum ClassDisplay
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
        dyRp.setFileName(DBProperties.getProperty(InventoryReport.class.getName() + ".FileName"));
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
        return new DynTransactionReport(this);
    }

    public static class DynTransactionReport
        extends AbstractDynamicReport
    {

        private final FilteredReport filteredReport;

        /**
         * @param _filteredReport reports
         */
        public DynTransactionReport(final FilteredReport _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> beans = new ArrayList<>();
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);

            AbstractGroupedByDate_Base.DateGroup dateGroup;
            if (filter.containsKey("dateGroup") && filter.get("dateGroup") != null) {
                dateGroup = (DateGroup) ((EnumFilterValue) filter.get("dateGroup")).getObject();
            } else {
                dateGroup = AbstractGroupedByDate_Base.DateGroup.MONTH;
            }
            final TransactionGroupedByDate groupedbyDate = new TransactionGroupedByDate();
            final DateTimeFormatter dateTimeFormatter = groupedbyDate.getDateTimeFormatter(dateGroup);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionAbstract);
            add2QueryBldr(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product)
                            .instance();
            final SelectBuilder selProdClass = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Product)
                            .clazz().type();
            if (!ClassDisplay.NONE.equals(getClassDisplay(_parameter))) {
                multi.addSelect(selProdClass);
            }
            final SelectBuilder selStorageInst = SelectBuilder.get().linkto(CIProducts.TransactionAbstract.Storage)
                            .instance();
            multi.addSelect(selProdInst, selStorageInst);
            multi.addAttribute(CIProducts.TransactionAbstract.Date, CIProducts.TransactionAbstract.Quantity,
                            CIProducts.TransactionAbstract.UoM);
            multi.execute();
            while (multi.next()) {
                final Instance transInst = multi.getCurrentInstance();
                final DateTime date = multi.getAttribute(CIProducts.TransactionAbstract.Date);
                final BigDecimal quantity = multi.getAttribute(CIProducts.TransactionAbstract.Quantity);
                final Instance prodInst = multi.getSelect(selProdInst);
                final Instance storageInst = multi.getSelect(selStorageInst);
                final String partial = groupedbyDate.getPartial(date, dateGroup).toString(dateTimeFormatter);
                final DataBean bean = new DataBean().setDateGroup(partial).setQuantity(quantity).setProdInst(prodInst)
                                .setTransInst(transInst).setStorageInst(storageInst);
                if (!ClassDisplay.NONE.equals(getClassDisplay(_parameter))) {
                    bean.setProdClasslist(multi.<List<Classification>>getSelect(selProdClass));
                }
                beans.add(bean);
            }
            return new JRBeanCollectionDataSource(beans);
        }

        protected void add2QueryBldr(final Parameter _parameter,
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

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();

            if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("storage", String.class)
                                .setHeaderWidth(150);
                crosstab.addRowGroup(rowGroup);
            } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("storage",
                                String.class);
                crosstab.addColumnGroup(columnGroup);
            }

            if (TypeDisplay.ROW.equals(getTypeDisplay(_parameter))) {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("type", String.class)
                                .setHeaderWidth(150);
                crosstab.addRowGroup(rowGroup);
            } else if (TypeDisplay.COLUMN.equals(getTypeDisplay(_parameter))) {
                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("type",
                                String.class);
                crosstab.addColumnGroup(columnGroup);
            }

            if (ClassDisplay.ROW.equals(getClassDisplay(_parameter))) {
                final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("classification",
                                String.class).setHeaderWidth(150);
                crosstab.addRowGroup(rowGroup);
            } else if (ClassDisplay.COLUMN.equals(getClassDisplay(_parameter))) {
                final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup(
                                "classification",
                                String.class);
                crosstab.addColumnGroup(columnGroup);
            }

            final CrosstabMeasureBuilder<BigDecimal> quantityMeasure = DynamicReports.ctab.measure(
                            getLabel("Column.Quantity"),
                            "quantity", BigDecimal.class, Calculation.SUM);
            final CrosstabMeasureBuilder<BigDecimal> quantityInMeasure = DynamicReports.ctab.measure(
                            getLabel("Column.QuantityIn"),
                            "quantityIn", BigDecimal.class, Calculation.SUM);
            final CrosstabMeasureBuilder<BigDecimal> quantityOutMeasure = DynamicReports.ctab.measure(
                            getLabel("Column.QuantityOut"),
                            "quantityOut", BigDecimal.class, Calculation.SUM);

            crosstab.addMeasure(quantityInMeasure, quantityOutMeasure, quantityMeasure);

            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("product", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("dateGroup",
                            String.class);

            crosstab.addColumnGroup(columnGroup);
            crosstab.setCellWidth(200);

            _builder.addSummary(crosstab);
        }

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

        protected TypeDisplay getTypeDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilteredReport().getFilterMap(_parameter).get(
                            "typeDisplay");
            TypeDisplay ret;
            if (filter != null) {
                ret = (TypeDisplay) filter.getObject();
            } else {
                ret = TypeDisplay.NONE;
            }
            return ret;
        }

        protected ClassDisplay getClassDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilteredReport().getFilterMap(_parameter).get(
                            "classDisplay");
            ClassDisplay ret;
            if (filter != null) {
                ret = (ClassDisplay) filter.getObject();
            } else {
                ret = ClassDisplay.NONE;
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

        protected String getLabel(final String _key)
        {
            return DBProperties.getProperty(TransactionReport.class.getName() + "." + _key);
        }

    }

    public static class DataBean
    {

        private String dateGroup;
        private BigDecimal quantity;
        private UoM uoM;

        private Instance prodInst;

        private Instance transInst;

        private Instance storageInst;
        private List<Classification> prodClasslist;

        /**
         * Getter method for the instance variable {@link #dateGroup}.
         *
         * @return value of instance variable {@link #dateGroup}
         */
        public String getDateGroup()
        {
            return this.dateGroup;
        }

        /**
         * Setter method for instance variable {@link #dateGroup}.
         *
         * @param _dateGroup value for instance variable {@link #dateGroup}
         */
        public DataBean setDateGroup(final String _dateGroup)
        {
            this.dateGroup = _dateGroup;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantity()
        {
            BigDecimal ret;
            if (getTransInst().getType().isCIType(CIProducts.TransactionOutbound)) {
                ret = this.quantity.negate();
            } else if (getTransInst().getType().isCIType(CIProducts.TransactionInbound)) {
                ret = this.quantity;
            } else {
                ret = BigDecimal.ZERO;
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
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantityIn()
        {
            BigDecimal ret;
            if (getTransInst().getType().isCIType(CIProducts.TransactionInbound)) {
                ret = this.quantity;
            } else {
                ret = BigDecimal.ZERO;
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #quantity}.
         *
         * @return value of instance variable {@link #quantity}
         */
        public BigDecimal getQuantityOut()
        {
            BigDecimal ret;
            if (getTransInst().getType().isCIType(CIProducts.TransactionOutbound)) {
                ret = this.quantity.negate();
            } else {
                ret = BigDecimal.ZERO;
            }
            return ret;
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
        public DataBean setUoM(final UoM _uoM)
        {
            this.uoM = _uoM;
            return this;
        }

        public String getProduct()
            throws EFapsException
        {
            final PrintQuery print = CachedPrintQuery.get4Request(getProdInst());
            print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description);
            print.execute();
            final String name = print.getAttribute(CIProducts.ProductAbstract.Name);
            final String decsr = print.getAttribute(CIProducts.ProductAbstract.Description);
            return name + " - " + decsr;
        }

        public String getType()
            throws EFapsException
        {
            return getProdInst().getType().getLabel();
        }

        /**
         * Getter method for the instance variable {@link #prodClasslist}.
         *
         * @return value of instance variable {@link #prodClasslist}
         */
        public List<Classification> getProdClasslist()
        {

            return this.prodClasslist;
        }

        public String getClassification()
            throws EFapsException
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

        public String getStorage()
            throws EFapsException
        {
            final PrintQuery print = CachedPrintQuery.get4Request(getStorageInst());
            print.addAttribute(CIProducts.StorageAbstract.Name);
            print.execute();
            return print.getAttribute(CIProducts.StorageAbstract.Name);
        }

        /**
         * Getter method for the instance variable {@link #prodInst}.
         *
         * @return value of instance variable {@link #prodInst}
         */
        public Instance getProdInst()
        {
            return this.prodInst;
        }

        /**
         * Setter method for instance variable {@link #prodInst}.
         *
         * @param _prodInst value for instance variable {@link #prodInst}
         */
        public DataBean setProdInst(final Instance _prodInst)
        {
            this.prodInst = _prodInst;
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
         */
        public DataBean setStorageInst(final Instance _storageInst)
        {
            this.storageInst = _storageInst;
            return this;
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
    }

    public static class TransactionGroupedByDate
        extends AbstractGroupedByDate
    {

    }
}
