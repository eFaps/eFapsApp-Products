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
package org.efaps.esjp.products.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.Command;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.products.Product;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;

import com.google.common.collect.Sets;

/**
 * @author The eFaps Team
 */
@EFapsUUID("26b08b80-b5d4-4a7a-ad02-6627d686a906")
@EFapsApplication("eFapsApp-Products")
public abstract class Variant_Base
    extends AbstractCommon
{

    /** The Constant FIELDPREFIX. */
    protected static final String FIELDPREFIX = "Variant_";

    /** The Constant CONTEXTKEY. */
    protected static final String CONTEXTKEY = Variant.class.getName() + ".ContextKey";

    /**
     * Creates the.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Product product = new Product()
        {

            @Override
            protected void add2Create(final Parameter _parameter,
                                      final Insert _insert)
                throws EFapsException
            {
                super.add2Create(_parameter, _insert);
                _insert.add(CIProducts.ProductVariantBase.VariantConfig, getVariantConf(_parameter));
            }
        };
        return product.create(_parameter);
    }

    /**
     * Edits the.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Product product = new Product()
        {

            @Override
            protected void add2Edit(final Parameter _parameter,
                                    final Update _update)
                throws EFapsException
            {
                super.add2Edit(_parameter, _update);
                _update.add(CIProducts.ProductVariantBase.VariantConfig, getVariantConf(_parameter));
            }
        };
        return product.edit(_parameter);
    }

    /**
     * Creates the variants.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return createVariants(final Parameter _parameter)
        throws EFapsException
    {
        @SuppressWarnings("unchecked")
        final Map<String, VariantWrapper> contextMap = (Map<String, VariantWrapper>) Context
                        .getThreadContext().getSessionAttribute(Variant.CONTEXTKEY);
        final String[] selected = _parameter.getParameterValues(Variant.FIELDPREFIX);
        if (selected != null) {
            for (final String key : selected) {
                final VariantWrapper variant = contextMap.get(key);
                final Map<String, Object> map = new HashMap<String, Object>();
                map.put(CIProducts.ProductStandart.Name.name,
                                getName4Variant(_parameter, _parameter.getCallInstance()));
                map.put(CIProducts.ProductStandart.Description.name,
                                getDescription4Variant(_parameter, _parameter.getCallInstance(), variant));

                for (final ElementWrapper ele : variant.getElements()) {
                    final Attribute attr = ele.getAttribute();
                    if (attr.getParent() instanceof Classification) {
                        map.put(attr.getParent().getName() + "_" + ele.getAttribute().getName(), ele.getObjInst());
                    }
                }
                final Instance cloneInst = new Product().cloneProduct(_parameter, _parameter.getCallInstance(),
                                CIProducts.ProductStandart.getType(), map, true);
                final Insert insert = new Insert(CIProducts.ProductVariantBase2Product);
                insert.add(CIProducts.ProductVariantBase2Product.FromLink, _parameter.getCallInstance());
                insert.add(CIProducts.ProductVariantBase2Product.ToLink, cloneInst);
                insert.execute();
            }
        }
        return new Return();
    }

    /**
     * Gets the name for variant.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _baseInst the _base inst
     * @return the name4 variant
     * @throws EFapsException on error
     */
    protected String getName4Variant(final Parameter _parameter,
                                     final Instance _baseInst)
        throws EFapsException
    {
        String ret = "";
        final PrintQuery print = new PrintQuery(_baseInst);
        print.addAttribute(CIProducts.ProductAbstract.Name);
        print.execute();
        final String baseName = print.<String>getAttribute(CIProducts.ProductAbstract.Name);

        final Set<String> names = new HashSet<>();
        final QueryBuilder relQueryBldr = new QueryBuilder(CIProducts.ProductVariantBase2Product);
        relQueryBldr.addWhereAttrEqValue(CIProducts.ProductVariantBase2Product.FromLink, _baseInst);
        final MultiPrintQuery multi = relQueryBldr.getPrint();
        final SelectBuilder sel = SelectBuilder.get().linkto(CIProducts.ProductVariantBase2Product.ToLink)
                        .attribute(CIProducts.ProductAbstract.Name);
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            names.add(multi.<String>getSelect(sel));
        }
        Integer idx = 1;
        ret = baseName + String.format("-%02d", idx);
        while (names.contains(ret)) {
            idx++;
            ret = baseName + String.format("-%02d", idx);
        }
        return ret;
    }

    /**
     * Gets the description for variant.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _baseInst the _base inst
     * @param _variant the _variant
     * @return the description4 variant
     * @throws EFapsException on error
     */
    protected String getDescription4Variant(final Parameter _parameter,
                                            final Instance _baseInst,
                                            final VariantWrapper _variant)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final PrintQuery print = new PrintQuery(_baseInst);
        print.addAttribute(CIProducts.ProductAbstract.Description);
        print.execute();
        final String baseDescription = print.<String>getAttribute(CIProducts.ProductAbstract.Description);
        ret.append(baseDescription);

        final Properties properties = Products.VARIANTCONFIG.get();

        for (final ElementWrapper ele : _variant.getElements()) {
            final String phrase = properties.getProperty(ele.getAttrKey() + ".Phrase4Description");
            final PrintQuery elePrint = new PrintQuery(ele.getObjInst());
            elePrint.addPhrase("Phrase", phrase);
            elePrint.execute();
            ret.append(elePrint.getPhrase("Phrase"));
        }
        return ret.toString();
    }


    /**
     * Gets the variant conf.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the variant conf
     * @throws EFapsException on error
     */
    protected VariantConf getVariantConf(final Parameter _parameter)
        throws EFapsException
    {
        final VariantConf ret = new VariantConf();
        for (final Entry<String, String[]> entry : _parameter.getParameters().entrySet()) {
            if (entry.getKey().startsWith(Variant.FIELDPREFIX)) {
                final VariantAttribute attr = new VariantAttribute();
                attr.setKey(entry.getKey().replaceFirst(Variant.FIELDPREFIX, ""));
                ret.getAttributes().add(attr);
                for (final String oid : entry.getValue()) {
                    final VariantElement element = new VariantElement();
                    attr.getElements().add(element);
                    element.setOid(oid);
                }
            }
        }
        return ret;
    }

    /**
     * Check access4 attribute.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return checkAccess4Attribute(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));
        boolean access = false;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            // if in a classification form during edit
            if (_parameter.getCallInstance() != null && _parameter.getCallInstance().isValid()) {
                access = _parameter.getCallInstance().getType().isCIType(CIProducts.ProductVariantBase);
            } else {
                final Command cmd = (Command) _parameter.get(ParameterValues.CALL_CMD);
                access = cmd != null && cmd.getTargetCreateType() != null
                                && cmd.getTargetCreateType().isCIType(CIProducts.ProductVariantBase);
            }
        } else {
            if (_parameter.getInstance().getType().isCIType(CIProducts.ProductVariantBase)) {
                access = true;
            } else if (_parameter.getInstance().getType() instanceof Classification) {
                final PrintQuery print = CachedPrintQuery.get4Request(_parameter.getInstance());
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(((Classification) _parameter.getInstance().getType()).getLinkAttributeName())
                                .instance();
                print.addSelect(sel);
                print.execute();
                access = print.<Instance>getSelect(sel).getType().isCIType(CIProducts.ProductVariantBase);
            }
        }
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Variant field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return variantFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Table table = new Table();
        final PrintQuery print = CachedPrintQuery.get4Request(_parameter.getInstance());
        print.addAttribute(CIProducts.ProductVariantBase.VariantConfig);
        print.execute();
        final VariantConf conf = print.getAttribute(CIProducts.ProductVariantBase.VariantConfig);
        final List<HashSet<ElementWrapper>> sets = new ArrayList<>();
        for (final VariantAttribute attribute : conf.getAttributes()) {
            final HashSet<ElementWrapper> set = new HashSet<ElementWrapper>();
            sets.add(set);
            for (final VariantElement ele : attribute.getElements()) {
                set.add(new ElementWrapper().setAttrKey(attribute.getKey()).setObjInst(Instance.get(ele.getOid())));
            }
        }
        final Set<List<ElementWrapper>> prod = Sets.cartesianProduct(sets);
        final List<VariantWrapper> variations = new ArrayList<>();
        for (final List<ElementWrapper> aprod : prod) {
            final VariantWrapper var = new VariantWrapper().setBaseInst(_parameter.getInstance());
            var.getElements().addAll(aprod);
            variations.add(var);
        }
        Collections.sort(variations);
        final boolean create = TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE));
        Map<String, VariantWrapper> contextMap = null;

        table.addRow();
        if (create) {
            contextMap = new HashMap<>();
            Context.getThreadContext().setSessionAttribute(Variant.CONTEXTKEY, contextMap);
            table.addHeaderColumn("");
        } else {
            table.addHeaderColumn(getDBProperty("ColumnHeader"));
        }
        for (final ElementWrapper ele : variations.get(0).getElements()) {
            table.addHeaderColumn(ele.getHeader());
        }
        for (final VariantWrapper variation : variations) {
            if (create && !variation.hasProduct() || !create) {
                table.addRow();
                if (create) {
                    final String key = RandomStringUtils.randomAlphanumeric(12);
                    contextMap.put(key, variation);
                    final StringBuilder bldr = new StringBuilder()
                        .append(" <input type=\"checkbox\" name=\"").append(FIELDPREFIX).append("\" value=\"")
                        .append(key).append("\" />");
                    table.addColumn(bldr.toString());
                } else {
                    table.addColumn(variation.getProductName());
                }
                for (final ElementWrapper ele : variation.getElements()) {
                    table.addColumn(ele.getColumn(_parameter));
                }
            }
        }
        ret.put(ReturnValues.SNIPLETT, table.toHtml());
        return ret;
    }

    /**
     * Gets the variant config field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the variant config field value
     * @throws EFapsException on error
     */
    public Return getVariantConfigFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final Properties properties = Products.VARIANTCONFIG.get();
        final String key = getProperty(_parameter, "Key");
        final String select = properties.getProperty(key + ".Select4UI");
        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter, properties, key);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addSelect(select);
        multi.execute();

        final Set<String> oids = new HashSet<>();
        if (TargetMode.VIEW.equals(_parameter.get(ParameterValues.ACCESSMODE))
                        || TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            VariantConf conf = null;
            if (_parameter.getInstance().getType().isCIType(CIProducts.ProductVariantBase)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_parameter.getInstance());
                print.addAttribute(CIProducts.ProductVariantBase.VariantConfig);
                print.execute();
                conf = print.getAttribute(CIProducts.ProductVariantBase.VariantConfig);
            } else if (_parameter.getInstance().getType() instanceof Classification) {
                final PrintQuery print = CachedPrintQuery.get4Request(_parameter.getInstance());
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(((Classification) _parameter.getInstance().getType()).getLinkAttributeName())
                                .attribute(CIProducts.ProductVariantBase.VariantConfig);
                print.addSelect(sel);
                print.execute();
                conf = print.getSelect(sel);
            }

            if (conf != null) {
                final VariantAttribute attr = conf.getAttribute(key);
                if (attr != null) {
                    for (final VariantElement ele : attr.getElements()) {
                        oids.add(ele.getOid());
                    }
                }
            }
        }

        while (multi.next()) {
            if (TargetMode.VIEW.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
                if (oids.contains(multi.getCurrentInstance().getOid())) {
                    html.append(StringEscapeUtils.escapeHtml4(String.valueOf(multi.getSelect(select)))).append("<br/>");
                }
            } else {
                html.append("<label><input type=\"checkbox\" name=\"").append(Variant.FIELDPREFIX)
                                .append(key).append("\" value=\"")
                                .append(multi.getCurrentInstance().getOid()).append("\" ");
                if (oids.contains(multi.getCurrentInstance().getOid())) {
                    html.append("checked=\"checked\"");
                }
                html.append("/>").append(StringEscapeUtils.escapeHtml4(String.valueOf(multi.getSelect(select))))
                                .append("</label><br/>");
            }
        }
        if (html.length() == 0) {
            html.append("&nbsp;");
        }
        ret.put(ReturnValues.SNIPLETT, html.toString());
        return ret;
    }

    /**
     * The Class VariantWrapper.
     */
    public static class VariantWrapper
        implements Comparable<VariantWrapper>
    {

        /** The elements. */
        private final Set<ElementWrapper> elements = new TreeSet<>();

        /** The base inst. */
        private Instance baseInst;

        /** The prod inst. */
        private Instance prodInst;

        /** The init. */
        private boolean init;

        /** The prod name. */
        private String prodName;

        /**
         * Initialize.
         *
         * @throws EFapsException on error
         */
        protected void initialize()
            throws EFapsException
        {
            if (!this.init) {
                this.init = true;
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
                final QueryBuilder relQueryBldr = new QueryBuilder(CIProducts.ProductVariantBase2Product);
                relQueryBldr.addWhereAttrEqValue(CIProducts.ProductVariantBase2Product.FromLink, getBaseInst());
                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                relQueryBldr.getAttributeQuery(CIProducts.ProductVariantBase2Product.ToLink));
                for (final ElementWrapper ele : getElements()) {
                    final Attribute attr = ele.getAttribute();
                    if (attr.getParent() instanceof Classification) {
                        final QueryBuilder attrQueryBldr = new QueryBuilder(attr.getParent());
                        attrQueryBldr.addWhereAttrEqValue(attr, ele.getObjInst());
                        queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                        attrQueryBldr.getAttributeQuery(((Classification) attr.getParent())
                                                        .getLinkAttributeName()));
                    }
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIProducts.ProductAbstract.Name);
                multi.execute();
                if (multi.next()) {
                    this.prodInst = multi.getCurrentInstance();
                    this.prodName = multi.getAttribute(CIProducts.ProductAbstract.Name);
                }
            }
        }

        /**
         * Getter method for the instance variable {@link #elements}.
         *
         * @return value of instance variable {@link #elements}
         */
        public Set<ElementWrapper> getElements()
        {
            return this.elements;
        }

        /**
         * Checks for product.
         *
         * @return true, if successful
         * @throws EFapsException on error
         */
        public boolean hasProduct()
            throws EFapsException
        {
            initialize();
            return this.prodInst != null && this.prodInst.isValid();
        }

        /**
         * Gets the product name.
         *
         * @return the product name
         * @throws EFapsException on error
         */
        public String getProductName()
            throws EFapsException
        {
            initialize();
            return hasProduct() ? this.prodName : "";
        }

        @Override
        public int compareTo(final VariantWrapper _variation0)
        {
            return getElements().iterator().next().compareTo(_variation0.getElements().iterator().next());
        }

        /**
         * Getter method for the instance variable {@link #baseInst}.
         *
         * @return value of instance variable {@link #baseInst}
         */
        public Instance getBaseInst()
        {
            return this.baseInst;
        }

        /**
         * Setter method for instance variable {@link #baseInst}.
         *
         * @param _baseInst value for instance variable {@link #baseInst}
         * @return the variant wrapper
         */
        public VariantWrapper setBaseInst(final Instance _baseInst)
        {
            this.baseInst = _baseInst;
            return this;
        }
    }

    /**
     * The Class ElementWrapper.
     */
    public static class ElementWrapper
        extends AbstractCommon
        implements Comparable<ElementWrapper>
    {

        /** The attr key. */
        private String attrKey;

        /** The obj inst. */
        private Instance objInst;

        /** The column. */
        private String column;

        /**
         * Gets the header.
         *
         * @return the header
         * @throws EFapsException on error
         */
        public CharSequence getHeader()
            throws EFapsException
        {
            String ret = "";
            final Properties properties = Products.VARIANTCONFIG.get();
            if (properties.containsKey(getAttrKey() + ".Header")) {
                ret = DBProperties.getProperty(properties.getProperty(getAttrKey() + ".Header"));
            }
            return ret;
        }

        /**
         * Gets the column.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return the column
         * @throws EFapsException on error
         */
        public String getColumn(final Parameter _parameter)
            throws EFapsException
        {
            if (this.column == null) {
                final Properties properties = Products.VARIANTCONFIG.get();
                final String select = properties.getProperty(getAttrKey() + ".Select4UI");
                final PrintQuery print =  CachedPrintQuery.get4Request(getObjInst());
                print.addSelect(select);
                print.execute();
                this.column = print.getSelect(select);
            }
            return this.column;
        }

        /**
         * Gets the sort string.
         *
         * @return the sort string
         */
        public String getSortString()
        {
            String sortPrefix = "";
            try {
                final Properties properties = Products.VARIANTCONFIG.get();
                sortPrefix = properties.getProperty(this.attrKey + ".SortPrefix", "");
            } catch (final EFapsException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return sortPrefix + getAttrKey();
        }

        /**
         * Getter method for the instance variable {@link #attrName}.
         *
         * @return value of instance variable {@link #attrName}
         */
        public String getAttrKey()
        {
            return this.attrKey;
        }

        /**
         * Gets the attribute.
         *
         * @return the attribute
         * @throws EFapsException on error
         */
        public Attribute getAttribute()
            throws EFapsException
        {
            final Properties properties = Products.VARIANTCONFIG.get();
            return Attribute.get(properties.getProperty(getAttrKey() + ".Attribute"));
        }

        /**
         * Setter method for instance variable {@link #attrName}.
         *
         * @param _attrName value for instance variable {@link #attrName}
         * @return the element wrapper
         */
        public ElementWrapper setAttrKey(final String _attrName)
        {
            this.attrKey = _attrName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #objInst}.
         *
         * @return value of instance variable {@link #objInst}
         */
        public Instance getObjInst()
        {
            return this.objInst;
        }

        /**
         * Setter method for instance variable {@link #objInst}.
         *
         * @param _objInst value for instance variable {@link #objInst}
         * @return the element wrapper
         */
        public ElementWrapper setObjInst(final Instance _objInst)
        {
            this.objInst = _objInst;
            return this;
        }

        @Override
        public int compareTo(final ElementWrapper _elementWrapper0)
        {
            return getSortString().compareTo(_elementWrapper0.getSortString());
        }
    }
}
