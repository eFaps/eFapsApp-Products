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

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.event.Parameter;
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
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Cost;
import org.efaps.esjp.products.Cost_Base.CostBean;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a70fc196-8b5b-4ae8-bcd6-757bb6cafe33")
@EFapsApplication("eFapsApp-Products")
public abstract class CostReport_Base
    extends FilteredReport
{
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
         * @param _costReport_Base
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

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
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
                            "origCurrency", DynamicReports.type.stringType());

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
                            "currency", DynamicReports.type.stringType());

            _builder
                .columnGrid(prodNameColumn, productDescrColumn, origGroup, costColumn, currencyColumn)
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
            return new DataBean(_parameter).setDate(date).setCurrencyInstance(currencyInst);
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
                this.costBean = new Cost().getCost(this.parameter, getDate(), getProductInst());
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
            final Instance currencyInstance = getCostBean().getCurrencyInstance();
            return currencyInstance != null && currencyInstance.isValid()
                            ? CurrencyInst.get(currencyInstance).getSymbol() : null;
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
    }
}
