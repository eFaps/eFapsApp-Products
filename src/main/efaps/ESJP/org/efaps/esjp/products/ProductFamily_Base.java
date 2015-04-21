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

package org.efaps.esjp.products;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("0622fda8-e7e2-45b0-93f6-3cb4df89d427")
@EFapsApplication("eFapsApp-Products")
public abstract class ProductFamily_Base
    extends AbstractCommon
{

    public static String CACHEKEY = ProductFamily.class.getName() + ".CacheKey";

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

    protected void add2create(final Parameter _parameter,
                              final Insert _insert)
        throws EFapsException
    {
        final Instance parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                        CIFormProducts.Products_ProductFamilyForm.parentOID.name))[0]);

        final PrintQuery print = new PrintQuery(parentInst);
        print.addAttribute(CIProducts.ProductFamilyAbstract.ProductLineLink);
        print.execute();

        _insert.add(CIProducts.ProductFamilyStandart.ParentLink, parentInst);
        _insert.add(CIProducts.ProductFamilyStandart.ProductLineLink,
                        print.getAttribute(CIProducts.ProductFamilyAbstract.ProductLineLink));

    }

    public Return getCodeUIFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, getCode(_parameter, _parameter.getInstance()));
        return ret;
    }

    public String getCode(final Parameter _parameter,
                          final Instance _inst)
        throws EFapsException
    {
        final StringBuilder strBldr = new StringBuilder();
        Instance inst = _inst;
        if (inst.getType().isKindOf(CIProducts.ProductAbstract)) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selFamInts = SelectBuilder.get().linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                            .instance();
            print.addSelect(selFamInts);
            print.execute();
            inst = print.getSelect(selFamInts);
        }

        while (!inst.getType().isCIType(CIProducts.ProductFamilyRoot)) {
            final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
            final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIProducts.ProductFamilyStandart.ParentLink)
                            .instance();
            print.addSelect(selParentInst);
            print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
            print.execute();
            inst = print.getSelect(selParentInst);
            strBldr.insert(0, print.getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
        }
        final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
        final SelectBuilder selLineCode = SelectBuilder.get().linkto(CIProducts.ProductFamilyAbstract.ProductLineLink)
                        .attribute(CIProducts.ProductLineAbstract.CodePart);
        print.addSelect(selLineCode);
        print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
        print.execute();
        strBldr.insert(0, print.getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
        strBldr.insert(0, print.getSelect(selLineCode));

        return strBldr.toString();
    }

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
        ret.put(ReturnValues.VALUES, map);
        return ret;
    }

    public Return productFamilyFieldFormat(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final IUIValue value = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        if (value.getObject() instanceof Instance) {
            final Instance inst = (Instance) value.getObject();
            ret.put(ReturnValues.VALUES, getName(_parameter, inst));
        }
        return ret;
    }

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


    public String getName(final Parameter _parameter,
                          final Instance _inst)
        throws EFapsException
    {
        final StringBuilder strBldr = new StringBuilder();
        Instance inst = _inst;
        if (inst.getType().isKindOf(CIProducts.ProductAbstract)) {
            final PrintQuery print = new PrintQuery(inst);
            final SelectBuilder selFamInts = SelectBuilder.get().linkto(CIProducts.ProductAbstract.ProductFamilyLink)
                            .instance();
            print.addSelect(selFamInts);
            print.execute();
            inst = print.getSelect(selFamInts);
        }
        boolean first = true;
        while (!inst.getType().isCIType(CIProducts.ProductFamilyRoot)) {
            final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
            final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIProducts.ProductFamilyStandart.ParentLink)
                            .instance();
            print.addSelect(selParentInst);
            print.addAttribute(CIProducts.ProductFamilyAbstract.Name);
            print.execute();
            inst = print.getSelect(selParentInst);
            if (first) {
                first = false;
            } else {
                strBldr.insert(0, " - ");
            }
            strBldr.insert(0, print.getAttribute(CIProducts.ProductFamilyAbstract.Name));
        }
        final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
        final SelectBuilder selLineCode = SelectBuilder.get().linkto(CIProducts.ProductFamilyAbstract.ProductLineLink)
                        .attribute(CIProducts.ProductLineAbstract.Name);
        print.addSelect(selLineCode);
        print.addAttribute(CIProducts.ProductFamilyAbstract.Name);
        print.execute();
        if (first) {
            first = false;
        } else {
            strBldr.insert(0, " - ");
        }
        strBldr.insert(0, print.getAttribute(CIProducts.ProductFamilyAbstract.Name));
        strBldr.insert(0, print.getSelect(selLineCode) + " - ");
        return strBldr.toString();
    }

    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<IWarning> warnings = new ArrayList<IWarning>();
        final String codePart = _parameter.getParameterValue(CIFormProducts.Products_ProductFamilyForm.codePart.name);
        final AbstractCommand cmd = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        // CreateMode and FamilyRoot
        if (cmd.getTargetCreateType() != null && cmd.getTargetCreateType().isCIType(CIProducts.ProductFamilyRoot)) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyRoot);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.CodePart, codePart);
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
            // check if the selected family has allready products assigned
            final QueryBuilder prodQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            prodQueryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ProductFamilyLink, parentInst);
            if (!prodQueryBldr.getQuery().execute().isEmpty()) {
                warnings.add(new FamilyHasProductsWarning());
            }

         // EditMode and FamilyRoot
        } else if (_parameter.getInstance().getType().isCIType(CIProducts.ProductFamilyRoot)) {
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductFamilyRoot);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductFamilyRoot.CodePart, codePart);
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
                            print.getAttribute(CIProducts.ProductFamilyStandart.ParentLink));
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
     * Warning for invalid name.
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
