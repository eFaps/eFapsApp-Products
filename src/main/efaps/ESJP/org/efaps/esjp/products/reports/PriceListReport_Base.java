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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.ProductFamily;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ColumnBuilder;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("d7679319-39ed-4b12-a7c2-97a6c6ad8954")
@EFapsApplication("eFapsApp-Products")
public abstract class PriceListReport_Base
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
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
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

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return new Report instance
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynPriceListReport(_parameter, this);
    }

    /**
     * Report.
     */
    public static class DynPriceListReport
        extends AbstractDynamicReport
    {

        /**
         * Report class.
         */
        private final PriceListReport_Base filteredReport;

        /**
         * List of PriceList type shown in the report.
         */
        private final List<Type> types = new ArrayList<>();

        /**
         * Show the classification in the report.
         */
        private boolean showClass;

        /**
         * Show only product marked as active.
         */
        private boolean activeProductsOnly;

        /** The show family. */
        private boolean showFamily;

        private boolean showBarcodes;

        /**
         * @param _parameter Parameter as passed from the eFaps API
         * @param _report Report this class is used in
         * @throws EFapsException on error
         */
        public DynPriceListReport(final Parameter _parameter,
                                  final PriceListReport_Base _report)
            throws EFapsException
        {
            filteredReport = _report;
            final Map<Integer, String> typesMap = analyseProperty(_parameter, "Type");
            for (final Entry<Integer, String> typeEntry : typesMap.entrySet()) {
                final String typeStr = typeEntry.getValue();
                final Type type = isUUID(typeStr) ? Type.get(UUID.fromString(typeStr)) : Type.get(typeStr);
                types.add(type);
            }
            final var props = Products.REPPRICELIST.get();
            showClass = "true".equalsIgnoreCase(props.getProperty("ShowClassification", "true"));
            showFamily = "true".equalsIgnoreCase(props.getProperty("ShowFamily", "true"));
            showBarcodes = Products.REPPRICELIST_ACTBARCODE.get();
            activeProductsOnly = "true".equalsIgnoreCase(props.getProperty("ActiveProductsOnly", "true"));
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
            final Map<String, Object> filter = filteredReport.getFilterMap(_parameter);
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

        @SuppressWarnings("unchecked")
        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final JRRewindableDataSource ret;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    LOG.error("Catched error", e);
                }
            } else {
                final Map<Instance, Map<String, ?>> values = new HashMap<>();

                final QueryBuilder priceListQueryBuilder = getQueryBldrFromProperties(_parameter);
                add2PriceListQueryBuilder(_parameter, priceListQueryBuilder);

                if (isActiveProductsOnly()) {
                    final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
                    attrQueryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);
                    final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID);
                    priceListQueryBuilder.addWhereAttrInQuery(CIProducts.ProductPricelistAbstract.ProductAbstractLink,
                                    attrQuery);
                }

                final AttributeQuery priceListQuery = priceListQueryBuilder.getAttributeQuery(
                                CIProducts.ProductPricelistAbstract.ID);

                final QueryBuilder posQueryBuilder = new QueryBuilder(CIProducts.ProductPricelistPosition);
                posQueryBuilder.addWhereAttrInQuery(CIProducts.ProductPricelistPosition.ProductPricelist,
                                priceListQuery);
                add2PosQueryBuilder(_parameter, posQueryBuilder);
                final MultiPrintQuery multi = posQueryBuilder.getPrint();
                final SelectBuilder selProd = SelectBuilder.get().linkto(
                                CIProducts.ProductPricelistPosition.ProductPricelist).linkto(
                                                CIProducts.ProductPricelistAbstract.ProductAbstractLink);
                final SelectBuilder selProdInst = new SelectBuilder(selProd).instance();
                final SelectBuilder selProdName = new SelectBuilder(selProd).attribute(CIProducts.ProductAbstract.Name);
                final SelectBuilder selProdDescr = new SelectBuilder(selProd).attribute(
                                CIProducts.ProductAbstract.Description);
                final SelectBuilder selProdUoM = new SelectBuilder(selProd).attribute(
                                CIProducts.ProductAbstract.DefaultUoM);
                final SelectBuilder selFamInst = new SelectBuilder(selProd)
                                .linkto(CIProducts.ProductAbstract.ProductFamilyLink).instance();
                final SelectBuilder selProdClass = new SelectBuilder(selProd).clazz().type();
                final SelectBuilder selCurrency = SelectBuilder.get().linkto(
                                CIProducts.ProductPricelistPosition.CurrencyId).attribute(CIERP.Currency.Symbol);
                final SelectBuilder selTypeInst = SelectBuilder.get().linkto(
                                CIProducts.ProductPricelistPosition.ProductPricelist).instance();

                final SelectBuilder selPricGrpInst = SelectBuilder.get()
                                .linkto(CIProducts.ProductPricelistPosition.PriceGroupLink).instance();
                if (Products.ACTIVATEPRICEGRP.get()) {
                    multi.addSelect(selPricGrpInst);
                }
                multi.addSelect(selProdInst, selProdName, selProdDescr, selProdUoM, selCurrency, selTypeInst);
                if (isShowClass()) {
                    multi.addSelect(selProdClass);
                }
                if (isShowFamily()) {
                    multi.addSelect(selFamInst);
                }
                multi.addAttribute(CIProducts.ProductPricelistPosition.Price);
                multi.execute();
                while (multi.next()) {
                    final Instance prodInst = multi.<Instance>getSelect(selProdInst);
                    final Map<String, Object> map;
                    if (values.containsKey(prodInst)) {
                        map = (Map<String, Object>) values.get(prodInst);
                    } else {
                        boolean skip = false;
                        map = new HashMap<>();
                        map.put("productOID", prodInst.getOid());
                        map.put("productName", multi.getSelect(selProdName));
                        map.put("productDescr", multi.getSelect(selProdDescr));
                        map.put("productDim", Dimension.getUoM(multi.<Long>getSelect(selProdUoM)).getName());

                        if (isShowClass()) {
                            final List<Classification> clazzes = multi.<List<Classification>>getSelect(selProdClass);
                            if (clazzes != null && !clazzes.isEmpty()) {
                                map.put("productClass", clazzes.get(0).getLabel());
                            } else {
                                map.put("productClass", "-");
                            }
                        }
                        if (isShowFamily()) {
                            final Instance famInst = multi.getSelect(selFamInst);
                            if (InstanceUtils.isValid(famInst)) {
                                final var props = Products.REPPRICELIST.get();
                                final var parameter = ParameterUtil.clone(_parameter);
                                if (props.containsKey("NameDefinition")) {
                                    ParameterUtil.setProperty(parameter, "NameDefinition",
                                                    props.getProperty("NameDefinition"));
                                }
                                if (props.containsKey("NameIncludeLine")) {
                                    ParameterUtil.setProperty(parameter, "NameIncludeLine",
                                                    props.getProperty("NameIncludeLine"));
                                }
                                if (props.containsKey("NameSeparator")) {
                                    ParameterUtil.setProperty(parameter, "NameSeparator",
                                                    props.getProperty("NameSeparator"));
                                }
                                map.put("productFamily", new ProductFamily().getName(parameter, famInst));
                            }
                        }
                        if (isShowBarcodes() && filterMap.containsKey("barCodeType")) {
                            final AttrDefFilterValue filter = (AttrDefFilterValue) filterMap.get("barCodeType");
                            final Set<Instance> selectedTypes = filter.getObject() == null ? Collections.emptySet() :filter.getObject();
                            final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                                            CIProducts.ProductAbstract.Barcodes.name);
                            // attrSet.getType();
                            final QueryBuilder barcodeQueryBldr = new QueryBuilder(attrSet);
                            barcodeQueryBldr.addWhereAttrEqValue(attrSet.getAttributeName(), prodInst);

                            final var barcodePrint = barcodeQueryBldr.getPrint();
                            barcodePrint.addAttribute("Code");
                            final SelectBuilder selBarcodeType = SelectBuilder.get().linkto("BarcodeType")
                                            .attribute("Value");
                            final SelectBuilder selBarcodeTypeInstance = SelectBuilder.get().linkto("BarcodeType")
                                            .instance();
                            barcodePrint.addSelect(selBarcodeType, selBarcodeTypeInstance);
                            barcodePrint.executeWithoutAccessCheck();
                            final var codes = new ArrayList<>();
                            final var types = new ArrayList<>();
                            map.put("productBarcodes", codes);
                            map.put("productBarcodeTypes", types);
                            while (barcodePrint.next()) {
                                final Instance barcodeTypeInstance = barcodePrint.getSelect(selBarcodeTypeInstance);
                                if (selectedTypes.contains(barcodeTypeInstance)) {
                                    codes.add(barcodePrint.getAttribute("Code"));
                                    types.add(barcodePrint.getSelect(selBarcodeType));
                                }
                            }
                            skip = codes.isEmpty() && !selectedTypes.isEmpty();
                        }
                        if (!skip) {
                            values.put(prodInst, map);
                        }
                    }
                    String key = "";
                    if (Products.ACTIVATEPRICEGRP.get()) {
                        final Instance priceGrpInst = multi.getSelect(selPricGrpInst);
                        key = priceGrpInst.isValid() ? priceGrpInst.getOid() : "";
                    }
                    final Instance listTypeInst = multi.<Instance>getSelect(selTypeInst);
                    map.put("price-" + listTypeInst.getType().getId() + "-" + key, multi.getAttribute(
                                    CIProducts.ProductPricelistPosition.Price));
                    map.put("currency-" + listTypeInst.getType().getId() + "-" + key, multi.getSelect(selCurrency));

                    add2Value(prodInst, map);
                }

                final List<Map<String, ?>> lstVal = new ArrayList<>(values.values());
                Collections.sort(lstVal, (_o1, _o2) -> _o1.get("productName").toString()
                                .compareTo(_o2.get("productName").toString()));
                ret = new JRMapCollectionDataSource(lstVal);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        protected void add2Value(final Instance _productInst, final Map<String, Object> _map)
            throws EFapsException
        {
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> productName = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productName"), "productName",
                            DynamicReports.type.stringType());
            @SuppressWarnings("rawtypes")
            final TextColumnBuilder<List> productBarcodeTypes = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productBarcodeTypes"), "productBarcodeTypes",
                            DynamicReports.type.listType());
            @SuppressWarnings("rawtypes")
            final TextColumnBuilder<List> productBarcodes = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productBarcodes"), "productBarcodes",
                            DynamicReports.type.listType());
            final TextColumnBuilder<String> productDescr = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productDescr"), "productDescr",
                            DynamicReports.type.stringType()).setWidth(350);
            final TextColumnBuilder<String> productClass = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productClass"), "productClass",
                            DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> productFamily = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productFamily"), "productFamily",
                            DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> productDim = DynamicReports.col.column(DBProperties
                            .getProperty(PriceListReport.class.getName() + ".productDim"), "productDim",
                            DynamicReports.type.stringType()).setWidth(50);

            final ComponentColumnBuilder linkColumn = FilteredReport.getLinkColumn(_parameter, "productOID");

            final List<ColumnGridComponentBuilder> grid = new ArrayList<>();

            if (getExType().equals(ExportType.HTML)) {
                _builder.addColumn(linkColumn);
                grid.add(linkColumn);
            }
            grid.add(productName);
            _builder.addColumn(productName);
            if (isShowBarcodes()) {
                final ColumnTitleGroupBuilder titleGroup = DynamicReports.grid.titleGroup(DBProperties
                                .getProperty(PriceListReport.class.getName() + ".productBarcodesTitle"),
                                productBarcodeTypes, productBarcodes);
                grid.add(titleGroup);
                _builder.addColumn(productBarcodeTypes, productBarcodes);
            }
            grid.add(productDescr);
            _builder.addColumn(productDescr);
            if (isShowClass()) {
                grid.add(productClass);
                _builder.addColumn(productClass);
            }
            if (isShowFamily()) {
                grid.add(productFamily);
                _builder.addColumn(productFamily);
            }
            grid.add(productDim);
            _builder.addColumn(productDim);

            final Map<Long, Set<Instance>> priceGrpMap = new HashMap<>();
            final Instance fakeInst = Instance.get("");
            if (Products.ACTIVATEPRICEGRP.get()) {
                final JRMapCollectionDataSource datasource = (JRMapCollectionDataSource) createDataSource(_parameter);
                for (final Map<String, ?> map : datasource.getData()) {
                    for (final Entry<String, ?> entry : map.entrySet()) {
                        if (entry.getKey().startsWith("price-")) {
                            final String[] val = entry.getKey().split("-");
                            final Long typeId = Long.valueOf(val[1]);
                            final Set<Instance> priceGrpSet;
                            if (priceGrpMap.containsKey(typeId)) {
                                priceGrpSet = priceGrpMap.get(typeId);
                            } else {
                                priceGrpSet = new HashSet<>();
                                priceGrpMap.put(typeId, priceGrpSet);
                            }
                            priceGrpSet.add(val.length == 3 ? Instance.get(val[2]) : fakeInst);
                        }
                    }
                }
            }

            for (final Type type : types) {
                final String title = DBProperties.getProperty(PriceListReport.class.getName() + "ColumnGroup." + type
                                .getName());
                final Set<Instance> priceGrpSet = priceGrpMap.get(type.getId());
                if (Products.ACTIVATEPRICEGRP.get() && priceGrpSet.size() > 1) {
                    final ColumnTitleGroupBuilder titleGroup = DynamicReports.grid.titleGroup(title);
                    grid.add(titleGroup);
                    for (final Instance priceGrpInst : priceGrpSet) {
                        String key = "";
                        String subTitle = "";
                        if (priceGrpInst.isValid()) {
                            key = priceGrpInst.getOid();
                            final PrintQuery print = new PrintQuery(priceGrpInst);
                            print.addAttribute(CIProducts.PriceGroupAbstract.Name);
                            print.execute();
                            subTitle = print.getAttribute(CIProducts.PriceGroupAbstract.Name);
                        }

                        final TextColumnBuilder<BigDecimal> price = DynamicReports.col.column(DBProperties.getProperty(
                                        PriceListReport.class.getName() + ".price"),
                                        "price-" + type.getId() + "-"
                                                        + key,
                                        DynamicReports.type.bigDecimalType());
                        final TextColumnBuilder<String> currency = DynamicReports.col.column(DBProperties.getProperty(
                                        PriceListReport.class.getName() + ".currency"),
                                        "currency-" + type.getId() + "-"
                                                        + key,
                                        DynamicReports.type.stringType()).setWidth(50);

                        final var colsBldrs = add2PriceColumnBldrs(price, currency);
                        _builder.addColumn(colsBldrs);
                        final ColumnTitleGroupBuilder subTitleGroup = DynamicReports.grid.titleGroup(subTitle, colsBldrs);
                        titleGroup.add(subTitleGroup);
                    }
                } else {
                    final TextColumnBuilder<BigDecimal> price = DynamicReports.col.column(DBProperties.getProperty(
                                    PriceListReport.class.getName() + ".price"), "price-" + type.getId() + "-",
                                    DynamicReports.type.bigDecimalType());
                    final TextColumnBuilder<String> currency = DynamicReports.col.column(DBProperties.getProperty(
                                    PriceListReport.class.getName() + ".currency"), "currency-" + type.getId() + "-",
                                    DynamicReports.type.stringType()).setWidth(50);
                    final var colsBldrs = add2PriceColumnBldrs(price, currency);
                    final ColumnTitleGroupBuilder titleGroup = DynamicReports.grid.titleGroup(title, colsBldrs);
                    grid.add(titleGroup);
                    _builder.addColumn(colsBldrs);
                }
            }
            _builder.columnGrid(grid.toArray(new ColumnGridComponentBuilder[grid.size()]));
        }

        protected ColumnBuilder<?, ?>[] add2PriceColumnBldrs(final TextColumnBuilder<BigDecimal> price,
                                                             final TextColumnBuilder<String> currency)
        {
            return new ColumnBuilder[] { price, currency };
        }

        /**
         * Getter method for the instance variable {@link #showClass}.
         *
         * @return value of instance variable {@link #showClass}
         */
        public boolean isShowClass()
        {
            return showClass;
        }

        /**
         * Setter method for instance variable {@link #showClass}.
         *
         * @param _showClass value for instance variable {@link #showClass}
         */
        public void setShowClass(final boolean _showClass)
        {
            showClass = _showClass;
        }

        /**
         * Getter method for the instance variable {@link #activeProductsOnly}.
         *
         * @return value of instance variable {@link #activeProductsOnly}
         */
        public boolean isActiveProductsOnly()
        {
            return activeProductsOnly;
        }

        /**
         * Setter method for instance variable {@link #activeProductsOnly}.
         *
         * @param _activeProductsOnly value for instance variable
         *            {@link #activeProductsOnly}
         */
        public void setActiveProductsOnly(final boolean _activeProductsOnly)
        {
            activeProductsOnly = _activeProductsOnly;
        }

        /**
         * Gets the report class.
         *
         * @return the report class
         */
        public PriceListReport_Base getFilteredReport()
        {
            return filteredReport;
        }

        /**
         * Checks if is show family.
         *
         * @return the show family
         */
        public boolean isShowFamily()
        {
            return showFamily;
        }

        /**
         * Sets the show family.
         *
         * @param _showFamily the new show family
         */
        public void setShowFamily(final boolean _showFamily)
        {
            showFamily = _showFamily;
        }

        public boolean isShowBarcodes()
        {
            return showBarcodes;
        }

        public void setShowBarcodes(final boolean showBarcodes)
        {
            this.showBarcodes = showBarcodes;
        }
    }
}
