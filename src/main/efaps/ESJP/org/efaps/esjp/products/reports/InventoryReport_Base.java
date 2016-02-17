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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.esjp.products.StorageGroup;
import org.efaps.esjp.products.TreeView;
import org.efaps.esjp.products.util.Products;
import org.efaps.ui.wicket.models.EmbeddedLink;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.ComponentColumnBuilder;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.component.GenericElementBuilder;
import net.sf.dynamicreports.report.builder.expression.AbstractComplexExpression;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("db37f9fd-a5ce-4feb-8c07-8932da68228a")
@EFapsApplication("eFapsApp-Products")
public abstract class InventoryReport_Base
    extends FilteredReport
{

    /**
     * The Enum StorageDisplay.
     */
    public enum StorageDisplay
    {
        /** show no storage. */
        NONE,
        /** Show as column. */
        COLUMN,
        /** Show as row. */
        ROW;
    }

    /**
     * The Enum TypeDisplay.
     */
    public enum TypeDisplay
    {
        /** Show no type. */
        NONE,
        /** Show as column. */
        COLUMN,
        /** Show as group. */
        GROUP;
    }

    /**
     * The Enum ClassDisplay.
     */
    public enum ClassDisplay
    {
        /** Show no classification. */
        NONE,
        /** Show as column. */
        COLUMN,
        /** Show as group. */
        GROUP;
    }

    /**
     * The Enum TypeDisplay.
     */
    public enum IndividualDisplay
    {
        /** Show only products. */
        PRODUCT,
        /** Show only individual products. */
        INDIVIDUAL,
        /** Show both. */
        BOTH;
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
        return new DynInventoryReport(this);
    }

    /**
     * Report class.
     */
    public static class DynInventoryReport
        extends AbstractDynamicReport
    {
        /**
         * The related filtered report.
         */
        private final FilteredReport filteredReport;

        /** The beans. */
        private List<InventoryBean> beans;

        /**
         * @param _filteredReport reports
         */
        public DynInventoryReport(final FilteredReport _filteredReport)
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
                if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    final List<Map<String, ?>> source = getMapList(_parameter);
                    ret = new JRMapCollectionDataSource(source);
                } else {
                    Collections.sort(getBeans(_parameter, null), getComparator(_parameter));
                    ret = new JRBeanCollectionDataSource(this.beans);
                }
                this.filteredReport.cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * Gets the map list.
         *
         * @param _parameter the _parameter
         * @return the map list
         * @throws EFapsException the e faps exception
         */
        protected List<Map<String, ?>> getMapList(final Parameter _parameter)
            throws EFapsException
        {
            final List<Map<String, ?>> ret = new ArrayList<>();
            final List<InventoryBean> tmpBeans = getBeans(_parameter, null);
            Collections.sort(tmpBeans, getComparator(_parameter));
            Map<String, Object> map = new HashMap<>();
            BigDecimal quantity = BigDecimal.ZERO;
            for (final InventoryBean bean : tmpBeans) {
                if (!bean.getProdName().equals(map.get("prodName"))) {
                    map = new HashMap<>();
                    quantity = BigDecimal.ZERO;
                    ret.add(map);
                    map.put("prodType", bean.getProdType());
                    map.put("prodClass", bean.getProdClass(Products.REPINVENTORYCLASSLEVEL.get()));
                    map.put("prodName", bean.getProdName());
                    map.put("prodDescr", bean.getProdDescr());
                    map.put("prodOID", bean.getProdOID());
                    map.put("currency", bean.getCurrency());
                    map.put("prodName", bean.getProdName());
                    map.put("cost", bean.getCost());
                    map.put("uoM", bean.getUoM());
                }
                quantity = quantity.add(bean.getQuantity()).add(bean.getReserved());
                map.put("total", bean.getCost().multiply(quantity));
                map.put(bean.getStorage() + "_quantity", bean.getQuantity());
                map.put(bean.getStorage() + "_reserved", bean.getReserved());
            }
            return ret;
        }

        /**
         * Gets the inventory object.
         *
         * @param _parameter the _parameter
         * @return the inventory object
         * @throws EFapsException the e faps exception
         */
        protected Inventory getInventoryObject(final Parameter _parameter)
            throws EFapsException
        {
            final Instance treeViewInst = getTreeViewInst(_parameter);

            return new Inventory()
            {

                @Override
                protected Type getInventoryType(final Parameter _parameter)
                    throws EFapsException
                {
                    Type ret;
                    if (Products.ACTIVATEINDIVIDUAL.get()) {
                        switch (getIndividualDisplay(_parameter)) {
                            case PRODUCT:
                                ret = CIProducts.Inventory.getType();
                                break;
                            case INDIVIDUAL:
                                ret = CIProducts.InventoryIndividual.getType();
                                break;
                            default:
                                ret = super.getInventoryType(_parameter);
                                break;
                        }
                    } else {
                        ret = super.getInventoryType(_parameter);
                    }
                    return ret;
                }

                @Override
                protected Type getTransactionType(final Parameter _parameter)
                    throws EFapsException
                {
                    Type ret;
                    if (Products.ACTIVATEINDIVIDUAL.get()) {
                        switch (getIndividualDisplay(_parameter)) {
                            case PRODUCT:
                                ret = CIProducts.TransactionInOutAbstract.getType();
                                break;
                            case INDIVIDUAL:
                                ret = CIProducts.TransactionIndividualAbstract.getType();
                                break;
                            default:
                                ret = super.getInventoryType(_parameter);
                                break;
                        }
                    } else {
                        ret = super.getInventoryType(_parameter);
                    }
                    return ret;
                }

                @Override
                protected InventoryBean getBean(final Parameter _parameter)
                    throws EFapsException
                {
                    return new ReportInventoryBean(treeViewInst);
                }
            };
        }

        /**
         * Gets the comparator.
         *
         * @param _parameter the _parameter
         * @return the comparator
         * @throws EFapsException the e faps exception
         */
        protected ComparatorChain<InventoryBean> getComparator(final Parameter _parameter)
            throws EFapsException
        {
            final ComparatorChain<InventoryBean> ret = new ComparatorChain<>();
            if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                ret.addComparator(new Comparator<InventoryBean>()
                {

                    @Override
                    public int compare(final InventoryBean _arg0,
                                       final InventoryBean _arg1)
                    {
                        return _arg0.getStorage().compareTo(_arg1.getStorage());
                    }
                });
            }
            ret.addComparator(new Comparator<InventoryBean>()
            {

                @Override
                public int compare(final InventoryBean _arg0,
                                   final InventoryBean _arg1)
                {
                    return _arg0.getProdName().compareTo(_arg1.getProdName());
                }
            });
            return ret;
        }

        /**
         * Gets the storage display.
         *
         * @param _parameter the _parameter
         * @return the storage display
         * @throws EFapsException the e faps exception
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
         * Gets the type display.
         *
         * @param _parameter the _parameter
         * @return the type display
         * @throws EFapsException the e faps exception
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
         * @param _parameter the _parameter
         * @return the class display
         * @throws EFapsException the e faps exception
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
         * Gets the storage display.
         *
         * @param _parameter the _parameter
         * @return the storage display
         * @throws EFapsException the e faps exception
         */
        protected IndividualDisplay getIndividualDisplay(final Parameter _parameter)
            throws EFapsException
        {
            final EnumFilterValue filter = (EnumFilterValue) getFilterMap(_parameter).get("individualDisplay");
            IndividualDisplay ret;
            if (filter != null) {
                ret = (IndividualDisplay) filter.getObject();
            } else {
                ret = IndividualDisplay.PRODUCT;
            }
            return ret;
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
         * Gets the currency inst.
         *
         * @param _parameter the _parameter
         * @return the currency inst
         * @throws EFapsException the e faps exception
         */
        protected Instance getTreeViewInst(final Parameter _parameter)
            throws EFapsException
        {
            Instance ret = null;
            final Map<String, Object> map = getFilterMap(_parameter);
            if (map.containsKey("productTreeView")) {
                final InstanceFilterValue filter = (InstanceFilterValue) map.get("productTreeView");
                if (filter != null) {
                    ret = filter.getObject();
                }
            }
            return ret;
        }

        /**
         * Gets the storage insts.
         *
         * @param _parameter the _parameter
         * @return the storage insts
         * @throws EFapsException the e faps exception
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
         * Gets the filter map.
         *
         * @param _parameter the _parameter
         * @return the filter map
         * @throws EFapsException the e faps exception
         */
        protected Map<String, Object> getFilterMap(final Parameter _parameter)
            throws EFapsException
        {
            return ((InventoryReport_Base) getFilteredReport()).getFilterMap(_parameter);
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final List<ColumnTitleGroupBuilder> groupBuilders = new ArrayList<>();
            ColumnGroupBuilder storageSumGroup = null;
            if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                final TextColumnBuilder<String> storageColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.storage"),
                                "storage", DynamicReports.type.stringType());
                _builder.addColumn(storageColumn);
                storageSumGroup = DynamicReports.grp.group(storageColumn);
                _builder.groupBy(storageSumGroup);
            }

            final ColumnTitleGroupBuilder prodGroup = DynamicReports.grid.titleGroup(
                            DBProperties.getProperty(InventoryReport.class.getName() + ".TitleGroup.product"));

            ColumnGroupBuilder typeGroup = null;
            if (!TypeDisplay.NONE.equals(getTypeDisplay(_parameter))) {
                final TextColumnBuilder<String> typeColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.prodType"),
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
                final TextColumnBuilder<String> classColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.prodClass"),
                                "prodClass", DynamicReports.type.stringType()).setWidth(250);
                _builder.addColumn(classColumn);

                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    classGroup = DynamicReports.grp.group(classColumn);
                    _builder.groupBy(classGroup);
                } else if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    prodGroup.add(classColumn);
                }
            }

            final Instance treeViewInst = getTreeViewInst(_parameter);
            if (treeViewInst != null && treeViewInst.isValid()) {
                final TextColumnBuilder<String> treeViewColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.treeView"),
                                "treeView", DynamicReports.type.stringType());
                _builder.addColumn(treeViewColumn);
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

            final TextColumnBuilder<String> prodNameColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodName"),
                            "prodName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> prodDescrColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.prodDescr"),
                            "prodDescr", DynamicReports.type.stringType()).setWidth(200);
            final TextColumnBuilder<String> uoMColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.uoM"),
                            "uoM", DynamicReports.type.stringType());
            _builder.addColumn(prodNameColumn, prodDescrColumn, uoMColumn);

            if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                prodGroup.add(prodNameColumn, prodDescrColumn, uoMColumn);
                groupBuilders.add(prodGroup);
                for (final String storage : getStorages(_parameter)) {
                    final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                                    .getProperty(InventoryReport.class.getName() + ".Column.quantity"),
                                    storage + "_quantity", DynamicReports.type.bigDecimalType());
                    final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(DBProperties
                                    .getProperty(InventoryReport.class.getName() + ".Column.reserved"),
                                    storage + "_reserved", DynamicReports.type.bigDecimalType());
                    final ColumnTitleGroupBuilder storageGroup = DynamicReports.grid.titleGroup(storage,
                                    quantityColumn,
                                    reservedColumn);
                    groupBuilders.add(storageGroup);
                    _builder.addColumn(quantityColumn, reservedColumn);
                }

            } else {
                final TextColumnBuilder<BigDecimal> quantityColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.quantity"),
                                "quantity", DynamicReports.type.bigDecimalType());
                final TextColumnBuilder<BigDecimal> reservedColumn = DynamicReports.col.column(DBProperties
                                .getProperty(InventoryReport.class.getName() + ".Column.reserved"),
                                "reserved", DynamicReports.type.bigDecimalType());
                _builder.addColumn(quantityColumn, reservedColumn);
            }

            final TextColumnBuilder<BigDecimal> costColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.cost"),
                            "cost", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.total"),
                            "total", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<String> currencyColumn = DynamicReports.col.column(DBProperties
                            .getProperty(InventoryReport.class.getName() + ".Column.currency"),
                            "currency", DynamicReports.type.stringType());

            if (getCurrencyInst(_parameter) != null) {
                _builder.addColumn(costColumn, totalColumn, currencyColumn);
                if (StorageDisplay.COLUMN.equals(getStorageDisplay(_parameter))) {
                    final ColumnTitleGroupBuilder costGroup = DynamicReports.grid.titleGroup(
                                    DBProperties.getProperty(InventoryReport.class.getName() + ".TitleGroup.cost"),
                                    costColumn, totalColumn, currencyColumn);
                    groupBuilders.add(costGroup);

                }
                if (TypeDisplay.GROUP.equals(getTypeDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(typeGroup, DynamicReports.sbt.sum(totalColumn));
                }
                if (ClassDisplay.GROUP.equals(getClassDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(classGroup, DynamicReports.sbt.sum(totalColumn));
                }
                if (StorageDisplay.ROW.equals(getStorageDisplay(_parameter))) {
                    _builder.addSubtotalAtGroupFooter(storageSumGroup, DynamicReports.sbt.sum(totalColumn));
                }
                _builder.addSubtotalAtColumnFooter(DynamicReports.sbt.sum(totalColumn));
            }
            if (!groupBuilders.isEmpty()) {
                _builder.columnGrid(groupBuilders.toArray(new ColumnTitleGroupBuilder[groupBuilders.size()]));
            }
        }

        /**
         * Gets the storages.
         *
         * @param _parameter the _parameter
         * @return the storages
         * @throws EFapsException the e faps exception
         */
        public List<String> getStorages(final Parameter _parameter)
            throws EFapsException
        {
            final List<String> ret = new ArrayList<>();
            for (final InventoryBean bean : getBeans(_parameter, null)) {
                if (!ret.contains(bean.getStorage())) {
                    ret.add(bean.getStorage());
                }
            }
            Collections.sort(ret);
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
         * Getter method for the instance variable {@link #beans}.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @param _inventory Inventory instance
         * @return value of instance variable {@link #beans}
         * @throws EFapsException on error
         */
        @SuppressWarnings("unchecked")
        public List<InventoryBean> getBeans(final Parameter _parameter,
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
                this.beans = (List<InventoryBean>) inventory.getInventory(_parameter);
            }
            return this.beans;
        }

        /**
         * Setter method for instance variable {@link #beans}.
         *
         * @param _beans value for instance variable {@link #beans}
         */
        public void setBeans(final List<InventoryBean> _beans)
        {
            this.beans = _beans;
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
     * The Class ReportInventoryBean.
     */
    public static class ReportInventoryBean
        extends InventoryBean
    {

        /** The tree view inst. */
        private final Instance treeViewInst;

        /**
         * Instantiates a new report inventory bean.
         *
         * @param _treeViewInst the _tree view inst
         */
        public ReportInventoryBean(final Instance _treeViewInst)
        {
            this.treeViewInst = _treeViewInst;
        }

        @Override
        public String getProdClass()
            throws EFapsException
        {
            return super.getProdClass(Products.REPINVENTORYCLASSLEVEL.get());
        }

        /**
         * Gets the tree view.
         *
         * @return the tree view
         * @throws EFapsException the e faps exception
         */
        public String getTreeView()
            throws EFapsException
        {
            String ret = null;
            if (getTreeViewInst() != null && getTreeViewInst().isValid()) {
                ret = TreeView.getTreeViewLabel(new Parameter(), getTreeViewInst(), getProdInstance(), true, true);
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #treeViewInst}.
         *
         * @return value of instance variable {@link #treeViewInst}
         */
        public Instance getTreeViewInst()
        {
            return this.treeViewInst;
        }
    }
}
