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

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.datamodel.ui.IUIValue;
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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.datatype.DateTimeDate;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("a70fc196-8b5b-4ae8-bcd6-757bb6cafe33")
@EFapsApplication("eFapsApp-Products")
public abstract class CostReport_Base
    extends FilteredReport
{

    /**
     * The Enum SrockFilter.
     */
    public enum StockFilter
    {
        /** Deactivate the FIlter. */
        NONE,
        /** Products that vae Stock. */
        HASSTOCK,
        /** Products that do not have Stock. */
        NOSTOCK;
    }

    /**
     * Generate report.
     *
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
     * Export report.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
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
     * Gets the cost type field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the cost type field value
     * @throws EFapsException on error
     */
    public Return getCostTypeFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final String key = value.getField().getName();
        final Map<String, Object> map = getFilterMap(_parameter);
        final String selected = map.containsKey(key) ? ((CostTypeFilterValue) map.get(key)).getObject() : "DEFAULT";
        final List<DropDownPosition> values = new ArrayList<DropDownPosition>();
        values.add(new Field.DropDownPosition("DEFAULT", new CostTypeFilterValue().getLabel(_parameter))
                        .setSelected(selected == null || "DEFAULT".equals(selected)));
        for (final CurrencyInst currencyInst : CurrencyInst.getAvailable()) {
            if (!currencyInst.getInstance().equals(Currency.getBaseCurrency())) {
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductCostAlternative);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductCostAlternative.CurrencyLink,
                                currencyInst.getInstance());
                queryBldr.setLimit(1);
                if (!queryBldr.getQuery().executeWithoutAccessCheck().isEmpty()) {
                    values.add(new Field.DropDownPosition(currencyInst.getInstance().getOid(), new CostTypeFilterValue()
                                    .setObject(currencyInst.getInstance().getOid()).getLabel(_parameter))
                                                    .setSelected(currencyInst.getInstance().getOid().equals(selected)));
                }
            }
        }
        ret.put(ReturnValues.VALUES, values);
        return ret;
    }

    /**
     * Gets the report.
     *
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynCostReport(this);
    }

    /**
     * Report class.
     */
    public static class DynCostReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final CostReport_Base filteredReport;

        /**
         * Instantiates a new dyn cost report.
         *
         * @param _filteredReport the _filtered report
         */
        public DynCostReport(final CostReport_Base _filteredReport)
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
                    LOG.error("Catched error", e);
                }
            } else {
                final List<DataBean> beans = new ArrayList<>();
                final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
                add2QueryBuilder(_parameter, queryBldr);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description);
                multi.execute();

                while (multi.next()) {
                    final DataBean bean = getDataBean(_parameter)
                        .setProductInst(multi.getCurrentInstance())
                        .setProductName(multi.<String>getAttribute(CIProducts.ProductAbstract.Name))
                        .setProductDescr(multi.<String>getAttribute(CIProducts.ProductAbstract.Description));
                    beans.add(bean);
                }
                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                chain.addComparator(new Comparator<DataBean>()
                {
                    @Override
                    public int compare(final DataBean _o1,
                                       final DataBean _o2)
                    {
                        return _o1.getProductName().compareTo(_o2.getProductName());
                    }
                });
                Collections.sort(beans, chain);
                ret = new JRBeanCollectionDataSource(beans);
                this.filteredReport.cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Add to query builder.
         *
         * @param _parameter the _parameter
         * @param _queryBldr the _query bldr
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = getFilteredReport().getFilterMap(_parameter);
            if (filter.containsKey("type")) {
                final TypeFilterValue typeFilter = (TypeFilterValue) filter.get("type");
                _queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Type, typeFilter.getObject().toArray());
            }
            switch (getStockFilter(_parameter)) {
                case HASSTOCK:
                    final QueryBuilder queryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                    _queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                    queryBldr.getAttributeQuery(CIProducts.InventoryAbstract.Product));
                    break;
                case NOSTOCK:
                    final QueryBuilder queryBldr2 = new QueryBuilder(CIProducts.InventoryAbstract);
                    _queryBldr.addWhereAttrNotInQuery(CIProducts.ProductAbstract.ID,
                                    queryBldr2.getAttributeQuery(CIProducts.InventoryAbstract.Product));
                    break;
                default:
                    break;
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final List<ColumnGridComponentBuilder> grid = new ArrayList<ColumnGridComponentBuilder>();

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);
            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
                grid.add(linkColumn);
            }

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.productName"),
                            "productName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> productDescrColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.productDescr"),
                            "productDescr", DynamicReports.type.stringType()).setFixedWidth(250);

            final TextColumnBuilder<BigDecimal> origCostColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.origCost"),
                            "origCost", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> origCurrencyColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.origCurrency"),
                            "origCurrency", DynamicReports.type.stringType()).setWidth(50);

            final TextColumnBuilder<DateTime> createdColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.created"),
                            "created",  DateTimeDate.get());
            final TextColumnBuilder<DateTime> validFromColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.validFrom"),
                            "validFrom",  DateTimeDate.get());
            final TextColumnBuilder<DateTime> validUntilColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.validUntil"),
                            "validUntil",  DateTimeDate.get());

            final ColumnTitleGroupBuilder origGroup = DynamicReports.grid.titleGroup(
                            getFilteredReport().getDBProperty("Group.origGroup"), origCostColumn, origCurrencyColumn,
                                createdColumn, validFromColumn, validUntilColumn);

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.cost"),
                            "cost", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.currency"),
                            "currency", DynamicReports.type.stringType()).setWidth(50);
            grid.addAll(Arrays.asList(prodNameColumn, productDescrColumn, origGroup, costColumn, currencyColumn));
            _builder
                .columnGrid(grid.toArray(new ColumnGridComponentBuilder[grid.size()]))
                .addColumn(prodNameColumn, productDescrColumn, origCostColumn, origCurrencyColumn, createdColumn,
                            validFromColumn, validUntilColumn, costColumn, currencyColumn);
        }

        /**
         * Gets the currency inst.
         *
         * @param _parameter the _parameter
         * @return the currency inst
         * @throws EFapsException the e faps exception
         */
        protected Instance getCurrencyInst(final Parameter _parameter)
            throws EFapsException
        {
            Instance ret = null;
            final Map<String, Object> map = getFilteredReport().getFilterMap(_parameter);
            if (map.containsKey("currency")) {
                final CurrencyFilterValue filter = (CurrencyFilterValue) map.get("currency");
                ret = filter.getObject();
            }
            return ret;
        }

        /**
         * Gets the stock filter.
         *
         * @param _parameter the _parameter
         * @return the stock filter
         * @throws EFapsException the eFaps exception
         */
        protected StockFilter getStockFilter(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilteredReport().getFilterMap(_parameter).get("stock");
            StockFilter ret;
            if (filter != null) {
                ret = (StockFilter) filter.getObject();
            } else {
                ret = StockFilter.NONE;
            }
            return ret;
        }


        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        protected CostReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Gets the data bean.
         *
         * @param _parameter the _parameter
         * @return the data bean
         * @throws EFapsException the efaps exception
         */
        protected DataBean getDataBean(final Parameter _parameter)
            throws EFapsException
        {
            final DateTime date = (DateTime) getFilteredReport().getFilterMap(_parameter).get("date");
            final Instance currencyInst = getCurrencyInst(_parameter);
            final Map<String, Object> map = getFilteredReport().getFilterMap(_parameter);
            final CostTypeFilterValue filterValue = (CostTypeFilterValue) map.get("costType");
            Instance alterInst = null;
            if (filterValue != null) {
                alterInst = Instance.get(filterValue.getObject());
            }
            return new DataBean(_parameter).setDate(date).setCurrencyInstance(currencyInst)
                            .setAlternativeInstance(alterInst);
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The product inst. */
        private Instance productInst;

        /** The product name. */
        private String productName;

        /** The product description. */
        private String productDescr;

        /** The cost. */
        private CostBean costBean;

        /** The parameter. */
        private final Parameter parameter;

        /** The date. */
        private DateTime date;

        /** The currency instance. */
        private Instance currencyInstance;

        /** The alternative instance. */
        private Instance alternativeInstance;

        /**
         * Instantiates a new data bean.
         *
         * @param _parameter the _parameter
         */
        public DataBean(final Parameter _parameter)
        {
            this.parameter = _parameter;
        }

        /**
         * Getter method for the instance variable {@link #productName}.
         *
         * @return value of instance variable {@link #productName}
         */
        public String getProductName()
        {
            return this.productName;
        }

        /**
         * Setter method for instance variable {@link #productName}.
         *
         * @param _productName value for instance variable {@link #productName}
         * @return the data bean
         */
        public DataBean setProductName(final String _productName)
        {
            this.productName = _productName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #productDescr}.
         *
         * @return value of instance variable {@link #productDescr}
         */
        public String getProductDescr()
        {
            return this.productDescr;
        }

        /**
         * Setter method for instance variable {@link #productDescr}.
         *
         * @param _productDescr value for instance variable {@link #productDescr}
         * @return the data bean
         */
        public DataBean setProductDescr(final String _productDescr)
        {
            this.productDescr = _productDescr;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #productInst}.
         *
         * @return value of instance variable {@link #productInst}
         */
        public Instance getProductInst()
        {
            return this.productInst;
        }

        /**
         * Setter method for instance variable {@link #productInst}.
         *
         * @param _productInst value for instance variable {@link #productInst}
         * @return the data bean
         */
        public DataBean setProductInst(final Instance _productInst)
        {
            this.productInst = _productInst;
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
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance;
        }

        /**
         * Setter method for instance variable {@link #currencyInstance}.
         *
         * @param _currencyInstance value for instance variable {@link #currencyInstance}
         * @return the data bean
         */
        public DataBean setCurrencyInstance(final Instance _currencyInstance)
        {
            this.currencyInstance = _currencyInstance;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #costBean}.
         *
         * @return value of instance variable {@link #costBean}
         * @throws EFapsException the e faps exception
         */
        public CostBean getCostBean()
            throws EFapsException
        {
            if (this.costBean == null) {
                if (getAlternativeInstance() == null) {
                    this.costBean = new Cost().getCost(this.parameter, getDate(), getProductInst());
                } else {
                    this.costBean = new Cost().getAlternativeCost(this.parameter, getDate(),
                                    getAlternativeInstance(), getProductInst());
                }

                if (this.costBean == null) {
                    this.costBean = new CostBean();
                }
            }
            return this.costBean;
        }

        /**
         * Setter method for instance variable {@link #costBean}.
         *
         * @param _costBean value for instance variable {@link #costBean}
         */
        public void setCostBean(final CostBean _costBean)
        {
            this.costBean = _costBean;
        }

        /**
         * Gets the orig cost.
         *
         * @return the orig cost
         * @throws EFapsException the e faps exception
         */
        public BigDecimal getOrigCost()
            throws EFapsException
        {
            return getCostBean().getCost();
        }

        /**
         * Gets the orig currency.
         *
         * @return the orig currency
         * @throws EFapsException the e faps exception
         */
        public String getOrigCurrency()
            throws EFapsException
        {
            final Instance ret = getCostBean().getCurrencyInstance();
            return ret != null && ret.isValid() ? CurrencyInst.get(ret).getSymbol() : null;
        }

        /**
         * Gets the created.
         *
         * @return the created
         * @throws EFapsException the e faps exception
         */
        public DateTime getCreated()
            throws EFapsException
        {
            return getCostBean().getCreated();
        }

        /**
         * Gets the valid from.
         *
         * @return the valid from
         * @throws EFapsException the e faps exception
         */
        public DateTime getValidFrom()
            throws EFapsException
        {
            return getCostBean().getValidFrom();
        }

        /**
         * Gets the valid until.
         *
         * @return the valid until
         * @throws EFapsException the e faps exception
         */
        public DateTime getValidUntil()
            throws EFapsException
        {
            return getCostBean().getValidUntil();
        }

        /**
         * Gets the cost.
         *
         * @return the cost
         * @throws EFapsException the e faps exception
         */
        public BigDecimal getCost()
            throws EFapsException
        {
            return getOrigCost() == null ? null : getCostBean().getCost4Currency(this.parameter, getCurrencyInstance());
        }

        /**
         * Gets the currency.
         *
         * @return the  currency
         * @throws EFapsException the e faps exception
         */
        public String getCurrency()
            throws EFapsException
        {
            return CurrencyInst.get(getCurrencyInstance()).getSymbol();
        }

        /**
         * Gets the product oid.
         *
         * @return the product oid
         * @throws EFapsException the e faps exception
         */
        public String getProductOID()
            throws EFapsException
        {
            return getProductInst().getOid();
        }

        /**
         * Getter method for the instance variable {@link #alternativeInstance}.
         *
         * @return value of instance variable {@link #alternativeInstance}
         */
        public Instance getAlternativeInstance()
        {
            return this.alternativeInstance;
        }

        /**
         * Setter method for instance variable {@link #alternativeInstance}.
         *
         * @param _alternativeInstance value for instance variable {@link #alternativeInstance}
         * @return the data bean
         */
        public DataBean setAlternativeInstance(final Instance _alternativeInstance)
        {
            if (_alternativeInstance != null && !_alternativeInstance.isValid()) {
                this.alternativeInstance = null;
            } else {
                this.alternativeInstance = _alternativeInstance;
            }
            return this;
        }
    }

    /**
     * Expression used to render a link for the UserInterface.
     */
    public static class LinkExpression
        extends AbstractComplexExpression<EmbeddedLink>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Costructor.
         */
        public LinkExpression()
        {
            addExpression(DynamicReports.field("productOID", String.class));
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
     * The Class CostTypeFilter.
     */
    public static class CostTypeFilterValue
        extends AbstractFilterValue<String>
    {
        /** The Constant serialVersionUID. */
        private static final long serialVersionUID = 1L;

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @return the label for this filter
         * @throws EFapsException on error
         */
        @Override
        public String getLabel(final Parameter _parameter)
            throws EFapsException
        {
            return getObject() == null || getObject() != null && !Instance.get(getObject()).isValid()
                            ? DBProperties.getProperty(CostReport.class.getName() + ".CostType.Standart")
                            : DBProperties.getFormatedDBProperty(
                                            CostReport.class.getName() + ".CostType.Alternative",
                                            (Object) CurrencyInst.get(Instance.get(getObject())).getName());
        }

        @Override
        public AbstractFilterValue<String> parseObject(final String[] _values)
        {
            if (!ArrayUtils.isEmpty(_values)) {
                setObject(_values[0]);
            }
            return super.parseObject(_values);
        }
    }
}
