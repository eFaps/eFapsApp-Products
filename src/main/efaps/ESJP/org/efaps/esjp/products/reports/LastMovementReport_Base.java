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

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.ProductsSettings;
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

    public enum StorageDisplay
    {
        NONE, COLUMN, GROUP;
    }

    public enum TypeDisplay
    {
        NONE, COLUMN, GROUP;
    }

    public enum ClassDisplay
    {
        NONE, COLUMN, GROUP;
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

        private final FilteredReport filteredReport;
        private List<DataBean> beans;

        /**
         * @param _lastMovementReport_Base
         */
        public DynLastMovementReport(final LastMovementReport_Base _lastMovementReport_Base)
        {
            this.filteredReport = _lastMovementReport_Base;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (this.filteredReport.isCached()) {
                ret = this.filteredReport.getDataSourceFromCache();
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
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                queryBldr.addWhereAttrLessValue(CIProducts.TransactionInOutAbstract.Date, date.plusMinutes(1));
                queryBldr.addWhereAttrGreaterValue(CIProducts.TransactionInOutAbstract.Date, mindate.minusMinutes(1));
                queryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product, prod2beans.keySet().toArray());
                queryBldr.addOrderByAttributeDesc(CIProducts.TransactionInOutAbstract.Date);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.setEnforceSorted(true);
                final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIProducts.TransactionInOutAbstract.Product)
                                .instance();
                multi.addSelect(selProdInst);
                multi.addAttribute(CIProducts.TransactionInOutAbstract.Date, CIProducts.TransactionInOutAbstract.Quantity);
                multi.execute();

                while (multi.next() && done.size() < tmpBeans.size()) {
                    final Instance prodInst = multi.getSelect(selProdInst);
                    if (!done.contains(prodInst)) {
                        final List<DataBean> beanList = prod2beans.get(prodInst);
                        final DateTime transDate = multi.getAttribute(CIProducts.TransactionInOutAbstract.Date);
                        BigDecimal quantity = multi.getAttribute(CIProducts.TransactionInOutAbstract.Quantity);
                        if (multi.getCurrentInstance().getType().isCIType(CIProducts.TransactionOutbound)) {
                            quantity = quantity.negate();
                        }
                        final Iterator<DataBean> beanIter = beanList.iterator();
                        final DataBean bean = beanIter.next();
                        bean.setDate(date);
                        if (bean.addMovement(transDate, quantity)) {
                            done.add(prodInst);
                        }
                        while (beanIter.hasNext()) {
                            beanIter.next().setLastDate(bean.getLastDate()).setDate(bean.getDate());
                        }
                    }
                }
                ret =  new JRBeanCollectionDataSource(tmpBeans);
                this.filteredReport.cache(ret);
            }
            return ret;
        }

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

        protected Map<String, Object> getFilterMap(final Parameter _parameter)
            throws EFapsException
        {
            return getFilteredReport().getFilterMap(_parameter);
        }

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

        protected void addConditionalStyle(final StyleBuilder _styleBldr)
            throws EFapsException
        {
            final Properties properties = Products.getSysConfig().getAttributeValueAsProperties(
                            ProductsSettings.LASTMOVEREP, true);
            int j = 1;
            while (properties.containsKey("color" + String.format("%02d", j))) {
                final ConditionExpression condition = new ConditionExpression();
                final ConditionalStyleBuilder conditionStyle = DynamicReports.stl.conditionalStyle(condition);
                conditionStyle.setForegroundColor(Color.decode(properties.getProperty("color"
                                + String.format("%02d", j))));
                if (properties.containsKey("greater" + String.format("%02d", j))) {
                    condition.setGreater(properties.getProperty("greater" + String.format("%02d", j)));
                }
                if (properties.containsKey("smaller" + String.format("%02d", j))) {
                    condition.setSmaller(properties.getProperty("smaller" + String.format("%02d", j)));
                }
                _styleBldr.addConditionalStyle(conditionStyle);
                j++;
            }
        }

        public Integer getMaxDays(final Parameter _parameter)
            throws EFapsException
        {
            final Properties properties = Products.getSysConfig().getAttributeValueAsProperties(
                            ProductsSettings.LASTMOVEREP, true);
            return Integer.parseInt(properties.getProperty("maxDays", "180"));
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
            };
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            ColumnGroupBuilder storageGroup = null;
            if (!StorageDisplay.NONE.equals(getStorageDisplay(_parameter))) {
                final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(getLabel("Column.storage"),
                                "storage", DynamicReports.type.stringType());
                _builder.addColumn(storageColumn);

                if (StorageDisplay.GROUP.equals(getStorageDisplay(_parameter))) {
                    storageGroup = DynamicReports.grp.group(storageColumn);
                    _builder.groupBy(storageGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    //
                }
            }

            final ColumnTitleGroupBuilder prodGroup = DynamicReports.grid.titleGroup(getLabel("TitleGroup.product"));

            ColumnGroupBuilder typeGroup = null;
            if (!TypeDisplay.NONE.equals(getTypeDisplay(_parameter))) {
                final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(getLabel("Column.prodType"),
                                "prodType", DynamicReports.type.stringType()).setWidth(150);
                _builder.addColumn(typeColumn);

                if (TypeDisplay.GROUP.equals(getTypeDisplay(_parameter))) {
                    typeGroup = DynamicReports.grp.group(typeColumn);
                    _builder.groupBy(typeGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(typeColumn);
                }
            }
            ColumnGroupBuilder classGroup = null;
            if (!ClassDisplay.NONE.equals(getClassDisplay(_parameter))) {
                final TextColumnBuilder<String> classColumn = DynamicReports.col.column(getLabel("Column.prodClass"),
                                "prodClass", DynamicReports.type.stringType()).setWidth(250);
                _builder.addColumn(classColumn);

                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    classGroup = DynamicReports.grp.group(classColumn);
                    _builder.groupBy(classGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(classColumn);
                }
            }
            if (getExType().equals(ExportType.HTML)) {
                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                "http://www.efaps.org", "efapslink")
                                .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new ProductLinkExpression())
                                .setHeight(12).setWidth(25);
                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
                _builder.addColumn(linkColumn);
                prodGroup.add(linkColumn);
            }

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(getLabel("Column.prodName"),
                            "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(getLabel("Column.prodDescr"),
                            "prodDescr", DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(getLabel("Column.uoM"),
                            "uoM", DynamicReports.type.stringType());
            _builder.addColumn(prodNameColumn, prodDescrColumn, uoMColumn);

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(getLabel("Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(getLabel("Column.reserved"),
                            "reserved", DynamicReports.type.bigDecimalType());
            _builder.addColumn(quantityColumn, reservedColumn);

            final StyleBuilder styleBldr = DynamicReports.stl.style().setBold(true);
            if (!getExType().equals(ExportType.PDF)) {
                styleBldr.setPadding(DynamicReports.stl.padding(10));
            }
            addConditionalStyle(styleBldr);

            final TextColumnBuilder<DateTime> lastDateColumn = DynamicReports.col.column(getLabel("Column.lastDate"),
                            "lastDate", DateTimeDate.get());

            final TextColumnBuilder<Integer> daysColumn = DynamicReports.col.column(getLabel("Column.days"),
                            "days", DynamicReports.type.integerType());
            daysColumn.setStyle(styleBldr);

            _builder.addColumn(lastDateColumn, daysColumn);

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(getLabel("Column.cost"),
                            "cost", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(getLabel("Column.total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(getLabel("Column.currency"),
                            "currency", DynamicReports.type.stringType());

            if (getCurrencyInst(_parameter) != null) {
                _builder.addColumn(costColumn, totalColumn, currencyColumn);
                _builder.addSubtotalAtColumnFooter(DynamicReports.sbt.sum(totalColumn));
            }
        }

        protected String getLabel(final String _key)
        {
            return DBProperties.getProperty(LastMovementReport.class.getName() + "." + _key);
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

    public static class DataBean
        extends InventoryBean
    {

        private DateTime lastDate;

        private DateTime date;

        private BigDecimal checkQuantity = BigDecimal.ZERO;

        /**
         * Getter method for the instance variable {@link #lastDate}.
         *
         * @return value of instance variable {@link #lastDate}
         */
        public DateTime getLastDate()
        {
            return this.lastDate;
        }

        /**
         * @param _transDate
         * @param _quantity
         */
        public boolean addMovement(final DateTime _transDate,
                                   final BigDecimal _quantity)
        {
            boolean ret = false;
            // first register
            if (this.lastDate == null) {
                setLastDate(_transDate);
                this.checkQuantity = _quantity;
            } else {
                // new register on the same day
                if (Days.daysBetween(getLastDate(), _transDate).getDays() == 0) {
                    this.checkQuantity = this.checkQuantity.add(_quantity);
                } else {
                    // the movements on one day equal ZERO ==> internal movement
                    if (this.checkQuantity.compareTo(BigDecimal.ZERO) == 0) {
                        setLastDate(_transDate);
                        this.checkQuantity = _quantity;
                    } else {
                        ret = true;
                    }
                }
            }
            return ret;
        }

        /**
         * Setter method for instance variable {@link #lastDate}.
         *
         * @param _lastDate value for instance variable {@link #lastDate}
         */
        public DataBean setLastDate(final DateTime _lastDate)
        {
            this.lastDate = _lastDate;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #days}.
         *
         * @return value of instance variable {@link #days}
         */
        public Integer getDays()
        {
            Integer ret = -1;
            if (getLastDate() != null && getDate() != null) {
                ret = Days.daysBetween(getLastDate(), getDate()).getDays();
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
         */
        public DataBean setDate(final DateTime _date)
        {
            this.date = _date;
            return this;
        }
    }

    public static class ConditionExpression
        extends AbstractSimpleExpression<Boolean>
    {

        private String greater;

        private String smaller;

        private static final long serialVersionUID = 1L;

        @Override
        public Boolean evaluate(final ReportParameters reportParameters)
        {
            boolean ret = true;
            final Integer days = reportParameters.getValue("days");

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
