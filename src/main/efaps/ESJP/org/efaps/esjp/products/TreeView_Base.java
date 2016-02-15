/*
 * Copyright 2003 - 2016 The eFaps Team
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

import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
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
@EFapsUUID("f1674c5a-9a87-4b3c-8d49-34172bee5cee")
@EFapsApplication("eFapsApp-Products")
public abstract class TreeView_Base
    extends AbstractCommon
{

    /**
     * Creates the TreeView Element.
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
     * @param _parameter the _parameter
     * @param _insert the _insert
     * @throws EFapsException the e faps exception
     */
    protected void add2create(final Parameter _parameter,
                              final Insert _insert)
        throws EFapsException
    {
        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        if (command != null && command.getTargetCreateType() != null) {
            if (command.getTargetCreateType().isCIType(CIProducts.TreeViewNode)) {
                final Instance parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                                CIFormProducts.Products_TreeViewNodeForm.parentOID.name))[0]);
                _insert.add(CIProducts.TreeViewNode.ParentLink, parentInst);
            } else if (command.getTargetCreateType().isCIType(CIProducts.TreeViewProduct)) {
                final Instance parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                                CIFormProducts.Products_TreeViewProductForm.parentOID.name))[0]);
                _insert.add(CIProducts.TreeViewProduct.ParentLink, parentInst);
            }
        }
    }

    /**
     * Creates the multiple products.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return createMultipleProducts(final Parameter _parameter)
        throws EFapsException
    {
        final Instance parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                        CIFormProducts.Products_ProductSearch4TreeViewForm.parentOID.name))[0]);
        final List<Instance> prodInstances = getSelectedInstances(_parameter);
        for (final Instance prodInst : prodInstances) {
            final PrintQuery print = new PrintQuery(prodInst);
            print.addAttribute(CIProducts.ProductAbstract.Name);
            print.executeWithoutAccessCheck();

            final Insert insert = new Insert(CIProducts.TreeViewProduct);
            insert.add(CIProducts.TreeViewProduct.ParentLink, parentInst);
            insert.add(CIProducts.TreeViewProduct.ProductLink, prodInst);
            insert.add(CIProducts.TreeViewProduct.Label, print.getAttribute(CIProducts.ProductAbstract.Name));
            insert.execute();
        }
        return new Return();
    }

}
