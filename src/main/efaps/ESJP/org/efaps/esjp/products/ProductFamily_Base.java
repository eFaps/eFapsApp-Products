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
package org.efaps.esjp.products;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.db.CachedInstanceQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.products.util.Products;
import org.efaps.util.EFapsException;

/**
 * TODO comment!.
 *
 * @author The eFaps Team
 */
@EFapsUUID("0622fda8-e7e2-45b0-93f6-3cb4df89d427")
@EFapsApplication("eFapsApp-Products")
public abstract class ProductFamily_Base
    extends AbstractCommon
{

    /**
     * The Enum NameDefintion.
     *
     * @author The eFaps Team
     */
    public enum NameDefinition
    {
        /** The whole path is shown. */
        ALL,

        /** Show only the last. */
        LAST;
    }


    /** The cachekey. */
    protected static final String CACHEKEY = ProductFamily.class.getName() + ".CacheKey";

    /**
     * Check4 sub families.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return check4SubFamilies(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyStandart);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.ParentLink, _parameter.getInstance());
        final boolean access = queryBldr.getQuery().execute().isEmpty();
        final boolean inverse = "true".equalsIgnoreCase(getProperty(_parameter, "Inverse"));
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

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
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                add2create(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Add to create.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _insert the insert
     * @throws EFapsException on error
     */
    @SuppressWarnings("unchecked")
    protected void add2create(final Parameter parameter,
                              final Insert insert)
        throws EFapsException
    {
        final Instance parentInst;
        if (parameter.get(ParameterValues.PAYLOAD) != null) {
            final var values = (Map<String, ?>) parameter.get(ParameterValues.PAYLOAD);
            final List<String> oids = (List<String>) values.get("eFapsSelectedOids");
            parentInst = Instance.get(oids.get(0));
        } else {
            parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                            CIFormProducts.Products_ProductFamilyForm.parentOID.name))[0]);
        }

        final PrintQuery print = new PrintQuery(parentInst);
        print.addAttribute(CIProducts.ProductFamilyAbstract.ProductLineLink);
        print.execute();

        insert.add(CIProducts.ProductFamilyStandart.ParentLink, parentInst);
        insert.add(CIProducts.ProductFamilyStandart.ProductLineLink,
                        print.<Long>getAttribute(CIProducts.ProductFamilyAbstract.ProductLineLink));
    }

    /**
     * Gets the code ui field value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the code ui field value
     * @throws EFapsException on error
     */
    public Return getCodeUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, getCode(_parameter, _parameter.getInstance()));
        return ret;
    }

    /**
     * Gets the code.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _inst the inst
     * @return the code
     * @throws EFapsException on error
     */
    public String getCode(final Parameter _parameter,
                          final Instance _inst)
        throws EFapsException
    {
        final StringBuilder strBldr = new StringBuilder();
        if (_inst != null && _inst.isValid()) {
            Instance inst = _inst;
            if (inst.getType().isKindOf(CIProducts.ProductAbstract)) {
                final PrintQuery print = new PrintQuery(inst);
                final SelectBuilder selFamInts = SelectBuilder.get()
                                .linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                                .instance();
                print.addSelect(selFamInts);
                print.execute();
                inst = print.getSelect(selFamInts);
            }
            if (inst.isValid()) {
                while (!inst.getType().isCIType(CIProducts.ProductFamilyRoot)) {
                    final PrintQuery print = new CachedPrintQuery(inst, ProductFamily.CACHEKEY);
                    final SelectBuilder selParentInst = SelectBuilder.get()
                                    .linkto(CIProducts.ProductFamilyStandart.ParentLink)
                                    .instance();
                    print.addSelect(selParentInst);
                    print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
                    print.execute();
                    inst = print.getSelect(selParentInst);
                    strBldr.insert(0, print.<String>getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
                }
                final PrintQuery print = new CachedPrintQuery(inst, ProductFamily.CACHEKEY);
                final SelectBuilder selLineCode = SelectBuilder.get()
                                .linkto(CIProducts.ProductFamilyAbstract.ProductLineLink)
                                .attribute(CIProducts.ProductLineAbstract.CodePart);
                print.addSelect(selLineCode);
                print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
                print.execute();
                strBldr.insert(0, print.<String>getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
                strBldr.insert(0, print.<String>getSelect(selLineCode));
            }
        }
        return strBldr.toString();
    }

    /**
     * Picker for family.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return picker4Family(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance famInst = Instance.get(_parameter.getParameterValue("selectedRow"));

        final String code = getCode(_parameter, famInst);
        final Map<String, Object> map = new HashMap<>();
        map.put(CIFormProducts.Products_ProductForm.namePrefix.name, code);
        map.put(CIFormProducts.Products_ProductForm.namePrefix4Edit.name, code);
        map.put(CIFormProducts.Products_ProductForm.nameSuffix.name,
                        new Product().getSuffix4Family(_parameter, famInst));
        map.put(CIFormProducts.Products_ProductForm.nameSuffix4Edit.name,
                        new Product().getSuffix4Family(_parameter, famInst));
        map.put(CIFormProducts.Products_ProductForm.productFamilyLink.name, famInst.getOid());
        map.put(CIFormProducts.Products_ProductForm.productFamily.name, getName(_parameter, famInst));
        ret.put(ReturnValues.VALUES, map);
        return ret;
    }

    /**
     * Gets the sets the selected family ui value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the sets the selected family ui value
     * @throws EFapsException on error
     */
    public Return getSetSelectedFamilyUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        Instance famInst = null;
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            final SelectBuilder selFamInst = SelectBuilder.get().linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                            .instance();
            print.addSelect(selFamInst);
            print.execute();
            famInst = print.getSelect(selFamInst);
        }
        if (famInst == null) {
            famInst = Instance.get("");
        }

        StringBuilder js = new StringBuilder()
            .append("topic.subscribe('eFaps/expand/")
                .append(CIFormProducts.Products_ProductFamilyPickerForm.strctBrws.name)
                .append("', function (e) {\n")
            .append("var nl = query('[name=\\'selectedRow\\']').forEach(function (node) {\n")
            .append(" if (query('.tree-junction-collapsed, .tree-junction-expanded ', ")
                .append("node.parentNode.parentNode).length > 0) {\n")
            .append("node.style =\"display:none\";\n")
            .append("} else {\n")
            .append("node.type='radio';\n")
            .append("}\n")
            .append("if (node.value == '").append(famInst.getOid()).append("') {\n")
            .append("domAttr.set(node, 'checked', true);\n")
            .append("}\n")
            .append("});\n")
            .append("});\n")
            .append("topic.subscribe('eFaps/collapse/")
                .append(CIFormProducts.Products_ProductFamilyPickerForm.strctBrws.name)
                .append("', function (e) {\n")
            .append("var nl = query('[name=\\'selectedRow\\']').forEach(function (node) {\n")
            .append(" if (query('.tree-junction-collapsed, .tree-junction-expanded ', ")
                .append("node.parentNode.parentNode).length > 0) {\n")
            .append("node.style =\"display:none\";\n")
            .append("} else {\n")
            .append("node.type='radio';\n")
            .append("}\n")
            .append("if (node.value == '").append(famInst.getOid()).append("') {\n")
            .append("domAttr.set(node, 'checked', true);\n")
            .append("}\n")
            .append("});\n")
            .append("});\n")
            .append("query('[name=\\'selectedRow\\']').forEach(function (node) {\n")
            .append(" if (query('.tree-junction-collapsed, .tree-junction-expanded ', ")
                .append("node.parentNode.parentNode).length > 0) {\n")
            .append("node.style =\"display:none\";\n")
            .append("} else {\n")
            .append("node.type='radio';\n")
            .append("}\n")
            .append("if (node.value == '").append(famInst.getOid()).append("') {\n")
            .append("domAttr.set(node, 'checked', true);\n")
            .append("}\n")
            .append(" });\n")
            .append(" query('[name=\\'selecteAll\\']').style(\"display\", \"none\");\n");
        js = InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.TOPIC, DojoLibs.QUERY, DojoLibs.DOMATTR);
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1000));
        return ret;
    }

    /**
     * Product family field format.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return productFamilyFieldFormat(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        if (value.getObject() instanceof Instance) {
            final Instance inst = (Instance) value.getObject();
            if (inst != null && inst.isValid()) {
                ret.put(ReturnValues.VALUES, getName(_parameter, inst));
            } else {
                ret.put(ReturnValues.VALUES, "");
            }
        }
        return ret;
    }

    /**
     * Gets the name.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _inst the inst
     * @return the name
     * @throws EFapsException on error
     */
    public String getName(final Parameter _parameter,
                          final Instance _inst)
        throws EFapsException
    {
        final String separator = getProperty(_parameter, "NameSeparator", Products.FAMILY_NAMESEP.get());

        final boolean includeLine = containsProperty(_parameter, "NameIncludeLine")
                        ?  Boolean.parseBoolean(getProperty(_parameter, "NameIncludeLine"))
                        :  Products.FAMILY_NAMEINCLLINE.get();

        final NameDefinition nameDef = containsProperty(_parameter, "NameDefinition")
                        ?  EnumUtils.getEnum(NameDefinition.class, getProperty(_parameter, "NameDefinition"))
                        :  Products.FAMILY_NAMEDEF.get();

        Instance inst = _inst;
        if (inst.getType().isKindOf(CIProducts.ProductAbstract)) {
            final PrintQuery print = CachedPrintQuery.get4Request(inst);
            final SelectBuilder selFamInts = SelectBuilder.get().linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                            .instance();
            print.addSelect(selFamInts);
            print.execute();
            inst = print.getSelect(selFamInts);
        }
        final List<String> parts = new ArrayList<>();
        while (InstanceUtils.isValid(inst) && !InstanceUtils.isType(inst, CIProducts.ProductFamilyRoot)) {
            final PrintQuery print = new CachedPrintQuery(inst, ProductFamily.CACHEKEY);
            final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIProducts.ProductFamilyStandart.ParentLink)
                            .instance();
            print.addSelect(selParentInst);
            print.addAttribute(CIProducts.ProductFamilyAbstract.Name);
            print.execute();
            inst = print.getSelect(selParentInst);
            parts.add(print.<String>getAttribute(CIProducts.ProductFamilyAbstract.Name));
        }

        if (InstanceUtils.isValid(inst)) {
            final PrintQuery print = new CachedPrintQuery(inst, ProductFamily.CACHEKEY);
            final SelectBuilder selLineName = SelectBuilder.get().linkto(CIProducts.ProductFamilyAbstract.ProductLineLink)
                            .attribute(CIProducts.ProductLineAbstract.Name);
            print.addSelect(selLineName);
            print.addAttribute(CIProducts.ProductFamilyAbstract.Name);
            print.execute();
            parts.add(print.<String>getAttribute(CIProducts.ProductFamilyAbstract.Name));

            if (includeLine) {
                parts.add(print.getSelect(selLineName));
            }
        }
        final List<String> finalParts;
        switch (nameDef) {
            case LAST:
                finalParts = parts.isEmpty() ? parts : parts.subList(0, 1);
                Collections.reverse(finalParts);
                break;
            case ALL:
            default:
                finalParts = parts;
                Collections.reverse(finalParts);
                break;
        }
        return finalParts.isEmpty() ? "" : StringUtils.join(finalParts, separator);
    }

    /**
     * Validate.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<IWarning> warnings = new ArrayList<>();
        final String codePart = _parameter.getParameterValue(CIFormProducts.Products_ProductFamilyForm.codePart.name);
        final AbstractCommand cmd = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        // CreateMode and FamilyRoot
        if (cmd.getTargetCreateType() != null && cmd.getTargetCreateType().isCIType(CIProducts.ProductFamilyRoot)) {
            final String prodLineId = _parameter.getParameterValue(
                            CIFormProducts.Products_ProductFamilyRootForm.productLineLink.name);
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyRoot);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.CodePart, codePart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.ProductLineLink, prodLineId);
            if (!queryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyCodeInvalidWarning());
            }
        // CreateMode and Family
        } else  if (cmd.getTargetCreateType() != null) {
            final Instance parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                            CIFormProducts.Products_ProductFamilyForm.parentOID.name))[0]);
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyStandart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.CodePart, codePart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.ParentLink, parentInst);
            if (!queryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyCodeInvalidWarning());
            }
            // check if the selected family has already products assigned
            final QueryBuilder prodQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            prodQueryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ProductFamilyLink, parentInst);
            if (!prodQueryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyHasProductsWarning());
            }

         // EditMode and FamilyRoot
        } else if (_parameter.getInstance().getType().isCIType(CIProducts.ProductFamilyRoot)) {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIProducts.ProductFamilyRoot.ProductLineLink);
            print.execute();
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyRoot);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.CodePart, codePart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.ProductLineLink,
                            print.<Long>getAttribute(CIProducts.ProductFamilyRoot.ProductLineLink));
            queryBldr.addWhereAttrNotEqValue(CIProducts.ProductFamilyRoot.ID, _parameter.getInstance());
            if (!queryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyCodeInvalidWarning());
            }
         // EditMode and Family
        } else {
            final PrintQuery print = new PrintQuery(_parameter.getInstance());
            print.addAttribute(CIProducts.ProductFamilyStandart.ParentLink);
            print.execute();
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyStandart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.CodePart, codePart);
            queryBldr.addWhereAttrNotEqValue(CIProducts.ProductFamilyStandart.ID, _parameter.getInstance());
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.ParentLink,
                            print.<Long>getAttribute(CIProducts.ProductFamilyStandart.ParentLink));
            if (!queryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyCodeInvalidWarning());
            }
        }

        if (warnings.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            ret.put(ReturnValues.SNIPLETT, WarningUtil.getHtml4Warning(warnings).toString());
            if (!WarningUtil.hasError(warnings)) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Gets the descendants.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _famInst the fam inst
     * @return the descendants
     * @throws EFapsException on error
     */
    protected static List<Instance> getDescendants(final Parameter _parameter,
                                                   final Instance _famInst)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final QueryBuilder queryBlr = new QueryBuilder(CIProducts.ProductFamilyStandart);
        queryBlr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.ParentLink, _famInst);
        final CachedInstanceQuery query = queryBlr.getCachedQuery(ProductFamily_Base.CACHEKEY);
        ret.addAll(query.execute());
        final List<Instance> tmp = new ArrayList<>();
        for (final Instance inst : ret) {
            tmp.addAll(ProductFamily.getDescendants(_parameter, inst));
        }
        ret.addAll(tmp);
        return ret;
    }


    /**
     * Gets the family instance for code.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _code the code
     * @return the family instance4 code
     * @throws EFapsException on error
     */
    protected static Instance getFamilyInstance4Code(final Parameter _parameter,
                                                     final String _code)
        throws EFapsException
    {
        Instance ret = null;
        final String code = _code.contains(".") ? _code.split(".")[0] : _code;
        int startIdx = 0;
        int curIdx = 1;
        String codePart = "";
        // search the ProductLine
        Instance lineInst = null;
        while (lineInst == null && curIdx < code.length() + 1) {
            codePart = code.substring(startIdx, curIdx);
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductLineStandart);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductLineStandart.CodePart, codePart);
            final List<Instance> insts = queryBldr.getQuery().execute();
            if (!insts.isEmpty() && insts.size() == 1) {
                lineInst = insts.get(0);
            } else {
                curIdx++;
            }
        }

        if (lineInst != null && lineInst.isValid()) {
            Instance rootInst = null;
            startIdx = curIdx;
            curIdx++;
            // search the root family
            while (rootInst == null && curIdx < code.length() + 1) {
                codePart = code.substring(startIdx, curIdx);
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyRoot);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.ProductLineLink, lineInst);
                queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.CodePart, codePart);
                final List<Instance> insts = queryBldr.getQuery().execute();
                if (!insts.isEmpty() && insts.size() == 1) {
                    rootInst = insts.get(0);
                } else {
                    curIdx++;
                }
            }
            if (rootInst != null && rootInst.isValid()) {
                startIdx = curIdx;
                curIdx++;
                Instance currentInst = rootInst;
                // search the sub family
                while (curIdx < code.length() + 1) {
                    codePart = code.substring(startIdx, curIdx);
                    final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyStandart);
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.ParentLink, currentInst);
                    queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyStandart.CodePart, codePart);
                    final List<Instance> insts = queryBldr.getQuery().execute();
                    if (!insts.isEmpty() && insts.size() == 1) {
                        currentInst = insts.get(0);
                        startIdx = curIdx;
                        curIdx++;
                    } else {
                        curIdx++;
                    }
                }
                ret = currentInst;
            }
        }
        return ret;
    }

    /**
     * Warning for invalid name.
     *
     * @author The eFaps Team
     */
    public static class FamilyCodeInvalidWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public FamilyCodeInvalidWarning()
        {
            setError(true);
        }
    }

    /**
     * Warning for invalid name.
     *
     * @author The eFaps Team
     */
    public static class FamilyHasProductsWarning
        extends AbstractWarning
    {
        /**
         * Constructor.
         */
        public FamilyHasProductsWarning()
        {
            setError(true);
        }
    }

}
