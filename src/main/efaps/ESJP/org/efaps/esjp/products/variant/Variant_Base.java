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

package org.efaps.esjp.products.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.attributetype.AbstractLinkType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.Command;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.esjp.ui.html.Table;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("26b08b80-b5d4-4a7a-ad02-6627d686a906")
@EFapsApplication("eFapsApp-Products")
public abstract class Variant_Base
    extends AbstractCommon
{

    protected final static String FIELDPREFIX = "Variant_";

    public enum Rule
    {
        SINGLE
    }

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final org.efaps.db.Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CIProducts.ProductVariantBase.VariantConfig, getVariantConf(_parameter));
            };
        };
        return create.execute(_parameter);
    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Edit create = new Edit()
        {

            @Override
            protected void add2MainUpdate(final Parameter _parameter,
                                          final Update _update)
                throws EFapsException
            {
                super.add2MainUpdate(_parameter, _update);
                _update.add(CIProducts.ProductVariantBase.VariantConfig, getVariantConf(_parameter));
            }
        };
        return create.execute(_parameter);
    }

    protected VariantConf getVariantConf(final Parameter _parameter)
        throws EFapsException
    {
        final VariantConf ret = new VariantConf();
        for (final Entry<String, String[]> entry : _parameter.getParameters().entrySet()) {
            if (entry.getKey().startsWith(Variant.FIELDPREFIX)) {
                final VariantAttribute attr = new VariantAttribute();
                attr.setName(entry.getKey().replaceFirst(Variant.FIELDPREFIX, ""));
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

    public Return checkAccess4Attribute(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));
        boolean access = false;
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final Command cmd = (Command) _parameter.get(ParameterValues.CALL_CMD);
            access = cmd != null && cmd.getTargetCreateType() != null
                            && cmd.getTargetCreateType().isCIType(CIProducts.ProductVariantBase);
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

    public Return variantFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Properties properties = Products.getSysConfig().getAttributeValueAsProperties(
                        ProductsSettings.VARIANTCONFIG, true);
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        final SelectBuilder selClass = SelectBuilder.get().clazz().type();
        print.addSelect(selClass);
        print.execute();
        final Object clazzes = print.getSelect(selClass);
        System.out.println(clazzes);
        final Table table = new Table();
        if (clazzes != null && clazzes instanceof List) {
            final List<Classification> clazzList = new ArrayList<>();
            for (final Object obj : (List<?>) clazzes) {
                Classification clazz = (Classification) obj;
                while (clazz != null) {
                    clazzList.add(clazz);
                    clazz = clazz.getParentClassification();
                }
            }
            Collections.reverse(clazzList);
            for (final Classification clazz : clazzList) {
                for (final Attribute attr : clazz.getAttributes().values()) {
                    if (properties.containsKey(attr.getKey() + ".Type")) {
                        if (attr.getAttributeType().getDbAttrType() instanceof AbstractLinkType) {
                            final String select = properties.getProperty(attr.getKey() + ".Select4UI");
                            final QueryBuilder queryBldr = new QueryBuilder(attr.getLink());
                            final MultiPrintQuery multi = queryBldr.getPrint();
                            multi.addSelect(select);
                            multi.execute();
                            while (multi.next()) {
                                table.addRow().addColumn("<input type=\"checkbox\" name=\"" + attr.getKey()
                                                + "\" value=\"" + multi.getCurrentInstance().getOid()
                                                + "\" />").addColumn(String.valueOf(multi.getSelect(select)));
                            }
                        } else {
                            table.addRow().addColumn(attr.getKey());
                        }
                    }
                }
            }
        }
        ret.put(ReturnValues.SNIPLETT, table.toHtml());
        return ret;
    }

    public Return getVariantConfigFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new StringBuilder();
        final Properties properties = Products.getSysConfig().getAttributeValueAsProperties(
                        ProductsSettings.VARIANTCONFIG, true);
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
                                .linkto( ((Classification) _parameter.getInstance().getType()).getLinkAttributeName())
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
}
