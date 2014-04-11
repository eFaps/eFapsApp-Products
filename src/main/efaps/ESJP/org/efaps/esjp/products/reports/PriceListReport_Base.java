/*
 * Copyright 2003 - 2014 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */


package org.efaps.esjp.products.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("d7679319-39ed-4b12-a7c2-97a6c6ad8954")
@EFapsRevision("$Rev$")
public abstract class PriceListReport_Base
    extends FilteredReport
{

        /**
         * @param _parameter    Parameter as passed by the eFasp API
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

        public Return exportReport(final Parameter _parameter)
            throws EFapsException
        {
            final Return ret = new Return();
            final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
            final String mime = (String) props.get("Mime");
            final AbstractDynamicReport dyRp = getReport(_parameter);
            dyRp.setFileName(DBProperties.getProperty(PriceListReport.class.getName() + ".FileName"));
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

        protected AbstractDynamicReport getReport(final Parameter _parameter)
            throws EFapsException
        {
            return new PLReport(_parameter, this);
        }

    public static class PLReport
        extends AbstractDynamicReport
    {
        private final PriceListReport_Base filteredReport;
        private final List<Type> types = new ArrayList<Type>();
        /**
         * @param _priceListReport_Base
         */
        public PLReport(final Parameter _parameter,
                        final PriceListReport_Base _report) throws EFapsException
        {
           this.filteredReport = _report;
           final Map<Integer, String> typesMap = analyseProperty(_parameter, "Type");
           for (final Entry<Integer, String> typeEntry : typesMap.entrySet()) {
               final String typeStr = typeEntry.getValue();
               final Type type = isUUID(typeStr) ? Type.get(UUID.fromString(typeStr)) : Type.get(typeStr);
               this.types.add(type);
           }
        }

        protected QueryBuilder getPriceListQueryBuilder(final Parameter _parameter)
            throws EFapsException
        {
            QueryBuilder ret = null;
            for (final Type type : this.types) {
                if (ret == null) {
                    ret = new QueryBuilder(type);
                } else {
                    ret.addType(type);
                }
            }
            return ret;
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
        protected void add2PriceListQueryBuilder(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filter = this.filteredReport.getFilterMap(_parameter);
            final DateTime date;
            if (filter.containsKey("date")) {
                date = (DateTime) filter.get("date");
            } else {
                date = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIProducts.ProductPricelistAbstract.ValidUntil,
                            date.withTimeAtStartOfDay().minusSeconds(1));
            _queryBldr.addWhereAttrLessValue(CIProducts.ProductPricelistAbstract.ValidFrom, date.plusDays(1)
                            .withTimeAtStartOfDay());
        }

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _queryBldr QueryBuilder the criteria will be added to
         * @throws EFapsException on error
         */
        protected void add2PosQueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {

        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<String> cols= new ArrayList<String>();
            if (getExType().equals(ExportType.HTML)) {
                cols.add("productOID");
            }
            cols.add("productName");
            cols.add("productDescr");
            cols.add("productDim");

            for (final Type type : this.types) {
                cols.add("price" + type.getId());
                cols.add("currency" + type.getId());
            }

            final DRDataSource dataSource = new DRDataSource(cols.toArray(new String[cols.size()]));

            final Map<Instance, Map<String, Object>> values = new HashMap<Instance, Map<String, Object>>();

            final QueryBuilder priceListQueryBuilder = getPriceListQueryBuilder(_parameter);
            add2PriceListQueryBuilder(_parameter, priceListQueryBuilder);

            if ("true".equalsIgnoreCase(getProperty(_parameter, "ActiveProductsOnly"))) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID);
                priceListQueryBuilder.addWhereAttrInQuery(CIProducts.ProductPricelistAbstract.ProductAbstractLink,
                                attrQuery);
            }

            final AttributeQuery priceListQuery = priceListQueryBuilder
                            .getAttributeQuery(CIProducts.ProductPricelistAbstract.ID);

            final QueryBuilder posQueryBuilder = new QueryBuilder(CIProducts.ProductPricelistPosition);
            posQueryBuilder.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist, priceListQuery);
            add2PosQueryBuilder(_parameter, posQueryBuilder);
            final MultiPrintQuery multi = posQueryBuilder.getPrint();
            final SelectBuilder selProd = SelectBuilder.get()
                            .linkto(CIProducts.ProductPricelistPosition.ProductPricelist)
                            .linkto(CIProducts.ProductPricelistAbstract.ProductAbstractLink);
            final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
            final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
            final SelectBuilder selProdDescr = new SelectBuilder(selProd)
                            .attribute(CIProducts.ProductAbstract.Description);
            final SelectBuilder selProdDim = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Dimension);
            final SelectBuilder selCurrency = SelectBuilder.get()
                            .linkto(CIProducts.ProductPricelistPosition.CurrencyId).attribute(CIERP.Currency.ISOCode);
            final SelectBuilder selTypeInst = SelectBuilder.get()
                            .linkto(CIProducts.ProductPricelistPosition.ProductPricelist).instance();
            multi.addSelect(selProdInst, selProdName, selProdDescr, selProdDim, selCurrency, selTypeInst);
            multi.addAttribute(CIProducts.ProductPricelistPosition.Price);
            multi.execute();
            while (multi.next()) {
                final Instance prodInst = multi.<Instance>getSelect(selProdInst);
                final Map<String, Object> map;
                if (values.containsKey(prodInst)) {
                    map = values.get(prodInst);
                } else {
                    map = new HashMap<String, Object>();
                    values.put(prodInst, map);
                    map.put("productOID", prodInst.getOid());
                    map.put("productName", multi.getSelect(selProdName));
                    map.put("productDescr", multi.getSelect(selProdDescr));
                    map.put("productDim", Dimension.get(multi.<Long>getSelect(selProdDim)).getName());
                }
                final Instance listTypeInst = multi.<Instance>getSelect(selTypeInst);
                 map.put("price" + listTypeInst.getType().getId(),
                                multi.getAttribute(CIProducts.ProductPricelistPosition.Price));
                map.put("currency" + listTypeInst.getType().getId(), multi.getSelect(selCurrency));
            }

            final ArrayList<Map<String, Object>> lstVal = new ArrayList<Map<String, Object>>(values.values());
            Collections.sort(lstVal, new Comparator<Map<String, Object>>() {

                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    return _o1.get("productName").toString().compareTo(_o2.get("productName").toString());
                }
            });

            for (final Map<String, Object> map : lstVal) {
                final List<Object> tmpList = new ArrayList<Object>();
                if (getExType().equals(ExportType.HTML)) {
                    tmpList.add(map.get("productOID"));
                }
                tmpList.add(map.get("productName"));
                tmpList.add(map.get("productDescr"));
                tmpList.add(map.get("productDim"));
                for (final Type type : this.types) {
                    tmpList.add(map.get("price" + type.getId()));
                    tmpList.add(map.get("currency" + type.getId()));
                }
                dataSource.add(tmpList.toArray());
            }
            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> productName = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productName"), "productName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> productDescr = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productDescr"), "productDescr",
                            DynamicReports.type.stringType()).setWidth(350);
            final TextColumnBuilder<String> productDim = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productDim"), "productDim",
                            DynamicReports.type.stringType());

            final GenericElementBuilder linkElement = DynamicReports.cmp.genericElement(
                            "http://www.efaps.org", "efapslink")
                            .addParameter(EmbeddedLink.JASPER_PARAMETERKEY, new LinkExpression())
                            .setHeight(12).setWidth(25);
            final ComponentColumnBuilder linkColumn = DynamicReports.col.componentColumn(linkElement).setTitle("");

            final List<ColumnGridComponentBuilder> grid = new ArrayList<ColumnGridComponentBuilder>();

            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
                grid.add(linkColumn);
            }
            grid.add(productName);
            grid.add(productDescr);
            grid.add(productDim);

            _builder.addColumn(productName, productDescr, productDim);

            for (final Type type : this.types) {
                final String title = DBProperties.getProperty(PriceListReport.class.getName() + "ColumnGroup."
                                + type.getName());
                final TextColumnBuilder<BigDecimal> price = DynamicReports.col.column(DBProperties
                                .getProperty(PriceListReport.class.getName() + ".price"), "price" + type.getId(),
                                DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<String> currency = DynamicReports.col.column(DBProperties
                                .getProperty(PriceListReport.class.getName() + ".currency"), "currency" + type.getId(),
                                DynamicReports.type.stringType());
                final ColumnTitleGroupBuilder titleGroup = DynamicReports.grid.titleGroup(title, price, currency);
                grid.add(titleGroup);
                _builder.addColumn(price, currency);
            }
            _builder.columnGrid(grid.toArray(new ColumnGridComponentBuilder[grid.size()]));
        }
    }

    public static class LinkExpression
    extends AbstractComplexExpression<EmbeddedLink>
{

    private static final long serialVersionUID = 1L;

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
}
