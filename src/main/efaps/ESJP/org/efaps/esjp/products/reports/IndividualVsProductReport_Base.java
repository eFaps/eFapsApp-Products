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
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport_Base.ExportType;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.component.SubreportBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
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
@EFapsUUID("3996b358-eeb5-4bca-acbc-5849d8df25a6")
@EFapsApplication("eFapsApp-Products")
public abstract class IndividualVsProductReport_Base
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
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty(IndividualVsProductReport.class.getName() + ".FileName"));
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
        return new IndividualVsProductDynReport(this);
    }

    /**
     * Report class.
     */
    public static class IndividualVsProductDynReport
        extends AbstractDynamicReport
    {

        /**
         * The related filtered report.
         */
        private final FilteredReport filteredReport;

        /**
         * @param _filteredReport reports
         */
        public IndividualVsProductDynReport(final FilteredReport _filteredReport)
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
                final Map<Instance, ProductBean> beans = new HashMap<>();
                // get individuals with its products that are in stock
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryAbstract);

                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductIndividualAbstract);
                queryBldr.addWhereAttrInQuery(CIProducts.ProductIndividual.ID,
                                attrQueryBldr.getAttributeQuery(CIProducts.InventoryAbstract.Product));
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selProd = SelectBuilder.get()
                                .linkfrom(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract)
                                .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd)
                                .attribute(CIProducts.ProductAbstract.Description);
                multi.addSelect(selProdInst, selProdName, selProdDescr);
                multi.addAttribute(CIProducts.ProductIndividual.Name);
                multi.execute();
                while (multi.next()) {
                    ProductBean prodBean;
                    final Instance prodInst = multi.getSelect(selProdInst);
                    if (beans.containsKey(prodInst)) {
                        prodBean = beans.get(prodInst);
                    } else {
                        prodBean = new ProductBean();
                        prodBean.setOID(prodInst.getOid())
                                        .setName(multi.<String>getSelect(selProdName))
                                        .setDescr(multi.<String>getSelect(selProdDescr));
                        beans.put(prodInst, prodBean);
                    }
                    final IndividualBean dataBean = new IndividualBean()
                                    .setOID(multi.getCurrentInstance().getOid())
                                    .setName(multi.<String>getAttribute(CIProducts.ProductIndividual.Name));
                    prodBean.getIndividuals().add(dataBean);
                }
                // get stock products that have no individual stock
                final QueryBuilder attQeryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract2IndividualAbstract);

                final QueryBuilder prodQueryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract);
                prodQueryBldr.addWhereAttrInQuery(CIProducts.StoreableProductAbstract.ID, attrQueryBldr.getAttributeQuery(
                                CIProducts.InventoryAbstract.Product));
                prodQueryBldr.addWhereAttrInQuery(CIProducts.StoreableProductAbstract.ID, attQeryBldr.getAttributeQuery(
                                CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract));
                prodQueryBldr.addWhereAttrNotEqValue(CIProducts.StoreableProductAbstract.ID, beans.keySet().toArray());
                final MultiPrintQuery multi2 = prodQueryBldr.getPrint();
                multi2.addAttribute(CIProducts.StoreableProductAbstract.Name, CIProducts.StoreableProductAbstract.Description);
                multi2.execute();
                while (multi2.next()) {
                    final Instance prodInst = multi2.getCurrentInstance();
                    if (!beans.containsKey(prodInst)) {
                        final ProductBean prodBean = new ProductBean();
                        prodBean.setOID(prodInst.getOid())
                            .setName(multi2.<String>getAttribute(CIProducts.StoreableProductAbstract.Name))
                            .setDescr(multi2.<String>getAttribute(CIProducts.StoreableProductAbstract.Description));
                        beans.put(prodInst, prodBean);
                    }
                }

                final List<ProductBean> values = new ArrayList<>(beans.values());
                Collections.sort(values, new Comparator<ProductBean>()
                {

                    @Override
                    public int compare(final ProductBean _arg0,
                                       final ProductBean _arg1)
                    {
                        return _arg0.getName().compareTo(_arg1.getName());
                    }
                });
                ret = new JRBeanCollectionDataSource(values);
                this.filteredReport.cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the filter map.
         *
         * @param _parameter the _parameter
         * @return the filter map
         * @throws EFapsException the e faps exception
         */
        protected Map<String, Object> getFilterMap(final Parameter _parameter)
            throws EFapsException
        {
            return ((IndividualVsProductReport_Base) getFilteredReport()).getFilterMap(_parameter);
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
                                              throws EFapsException
        {

            if (getExType().equals(ExportType.HTML)) {
                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                "http://www.efaps.org", "efapslink")
                                .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new ProductLinkExpression())
                                .setHeight(12).setWidth(25);
                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
                _builder.addColumn(linkColumn);
            }

            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.name"),
                            "name", DynamicReports.type.stringType()).setWidth(200);

            final TextColumnBuilder<String> descrColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.descr"),
                            "descr", DynamicReports.type.stringType()).setWidth(200);

            final SubreportBuilder subreport = DynamicReports.cmp.subreport(new SubreportBuilderExp(this, getExType()))
                            .setDataSource(new SubreportDataExp());
            final ComponentColumnBuilder subReportColumn = DynamicReports.col.componentColumn(
                            getFilteredReport().getDBProperty("Column.Individuals"), subreport)
                            .setWidth(300);
            _builder.addField("individuals", Collection.class);

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.quantity"),
                            "quantity", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Column.uoM"), "uoM", DynamicReports.type.stringType());

            _builder.addColumn(nameColumn, descrColumn, quantityColumn, uoMColumn, subReportColumn);
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

        @Override
        protected void configure4Html(final Parameter _parameter)
            throws EFapsException
        {
            super.configure4Html(_parameter);
            getStyleTemplate().setHighlightDetailOddRows(false);
        }
    }

    public static class ProductBean
    {

        private final List<IndividualBean> individuals = new ArrayList<>();

        private String oid;

        private String uoM;
        private String name;
        private String descr;

        private BigDecimal quantity;

        private void init()
        {
            if (this.quantity == null) {
                final Parameter parameter = new Parameter();
                try {
                    for (final InventoryBean invBean : new Inventory()
                    {

                        @Override
                        protected void add2QueryBuilder(final Parameter _parameter,
                                                        final QueryBuilder _queryBldr)
                                                            throws EFapsException
                        {
                            _queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, Instance.get(getOID()));
                        };

                    }.getInventory(parameter)) {
                        this.quantity = invBean.getQuantity();
                        this.uoM = invBean.getUoM();
                    }
                } catch (final EFapsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public String getOID()
        {
            return this.oid;
        }

        public ProductBean setOID(final String _oid)
        {
            this.oid = _oid;
            return this;
        }

        public String getUoM()
        {
            init();
            return this.uoM;
        }

        public ProductBean setUoM(final String _uoM)
        {
            this.uoM = _uoM;
            return this;
        }

        public String getDescr()
        {
            return this.descr;
        }

        public ProductBean setDescr(final String _descr)
        {
            this.descr = _descr;
            return this;
        }

        public String getName()
        {
            return this.name;
        }

        public ProductBean setName(final String _name)
        {
            this.name = _name;
            return this;
        }

        public BigDecimal getQuantity()
        {
            init();
            return this.quantity;
        }

        public List<IndividualBean> getIndividuals()
        {
            Collections.sort(this.individuals, new Comparator<IndividualBean>() {

                @Override
                public int compare(final IndividualBean _arg0,
                                   final IndividualBean _arg1)
                {
                    return _arg0.getName().compareTo(_arg1.getName());
                }});
            return this.individuals;
        }
    }

    public static class IndividualBean
    {

        private String oid;
        private String name;

        private BigDecimal quantity;

        private void init()
        {
            if (this.quantity == null) {
                final Parameter parameter = new Parameter();
                try {
                    for (final InventoryBean invBean : new Inventory()
                    {

                        @Override
                        protected void add2QueryBuilder(final Parameter _parameter,
                                                        final QueryBuilder _queryBldr)
                                                            throws EFapsException
                        {
                            _queryBldr.addType(CIProducts.InventoryIndividual);
                            _queryBldr.addWhereAttrEqValue(CIProducts.Inventory.Product, Instance.get(getOID()));
                        };

                    }.getInventory(parameter)) {
                        this.quantity = invBean.getQuantity();

                    }
                } catch (final EFapsException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public String getName()
        {
            return this.name;
        }

        public IndividualBean setName(final String _name)
        {
            this.name = _name;
            return this;
        }

        public BigDecimal getQuantity()
        {
            init();
            return this.quantity;
        }

        public IndividualBean setQuantity(final BigDecimal _quantity)
        {
            this.quantity = _quantity;
            return this;
        }

        public String getOID()
        {
            return this.oid;
        }

        public IndividualBean setOID(final String _oid)
        {
            this.oid = _oid;
            return this;
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
            addExpression(DynamicReports.field("OID", String.class));
        }

        @Override
        public EmbeddedLink evaluate(final List<?> _values,
                                     final ReportParameters _reportParameters)
        {
            final String oid = (String) _values.get(0);
            return EmbeddedLink.getJasperLink(oid);
        }
    }

    public static class SubreportBuilderExp
        extends AbstractSimpleExpression<JasperReportBuilder>
    {

        private static final long serialVersionUID = 1L;

        /**
         * The related filtered report.
         */
        private final IndividualVsProductDynReport dynReport;

        private final ExportType exportType;

        public FilteredReport getFilteredReport()
        {
            return this.dynReport.getFilteredReport();
        }

        public SubreportBuilderExp(final IndividualVsProductDynReport _dynReport,
                                   final ExportType _exportType)
        {
            this.dynReport = _dynReport;
            this.exportType = _exportType;
        }

        @Override
        public JasperReportBuilder evaluate(final ReportParameters reportParameters)
        {
            final JasperReportBuilder report = DynamicReports.report().setShowColumnTitle(false)
                            .setHighlightDetailOddRows(false);;

            if (this.exportType.equals(ExportType.HTML)) {
                final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                                "http://www.efaps.org", "efapslink")
                                .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new ProductLinkExpression())
                                .setHeight(12).setWidth(25);
                final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");
                report.addColumn(linkColumn);
            }

            final TextColumnBuilder<String> nameColumn = DynamicReports.col.column(
                            "name", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(
                            "quantity", DynamicReports.type.bigDecimalType());

            report.addColumn(nameColumn, quantityColumn);
            return report;
        }
    }

    public static class SubreportDataExp
        extends AbstractSimpleExpression<JRDataSource>
    {

        private static final long serialVersionUID = 1L;

        @Override
        public JRDataSource evaluate(final ReportParameters reportParameters)
        {
            final Collection<IndividualBean> value = reportParameters.getValue("individuals");
            return new JRBeanCollectionDataSource(value);
        }
    }
}
