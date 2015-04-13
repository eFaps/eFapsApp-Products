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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.uiform.Create;
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
        final StringBuilder strBldr = new StringBuilder();
        Instance inst = _parameter.getInstance();
        while (!inst.getType().isCIType(CIProducts.ProductFamilyRoot)) {
            final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
            final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIProducts.ProductFamilyStandart.ParentLink)
                            .instance();
            print.addSelect(selParentInst);
            print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
            print.execute();
            inst = print.getSelect(selParentInst);
            strBldr.append(print.getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
        }
        final PrintQuery print = new CachedPrintQuery(inst, CACHEKEY);
        final SelectBuilder selLineCode = SelectBuilder.get().linkto(CIProducts.ProductFamilyAbstract.ProductLineLink)
                        .attribute(CIProducts.ProductLineAbstract.CodePart);
        print.addSelect(selLineCode);
        print.addAttribute(CIProducts.ProductFamilyAbstract.CodePart);
        print.execute();
        strBldr.append(print.getAttribute(CIProducts.ProductFamilyAbstract.CodePart));
        strBldr.append(print.getSelect(selLineCode));

        ret.put(ReturnValues.VALUES, strBldr.reverse().toString());
        return ret;
    }

}
