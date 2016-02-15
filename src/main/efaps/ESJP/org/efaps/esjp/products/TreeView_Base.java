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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

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
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.ui.wicket.util.EFapsKey;
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

    /** The Constant CACHEKEY. */
    protected static final String CACHEKEY = TreeView.class.getName() + ".CacheKey";

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
        Instance parentInst = _parameter.getInstance();
        if (parentInst == null || parentInst != null && !parentInst.getType().isKindOf(CIProducts.TreeViewAbstract)) {
            parentInst = Instance.get(((String[]) Context.getThreadContext().getSessionAttribute(
                        CIFormProducts.Products_ProductSearch4TreeViewForm.parentOID.name))[0]);
        }
        final List<Instance> prodInstances = getSelectedInstances(_parameter);
        for (final Instance prodInst : prodInstances) {
            final PrintQuery print = new PrintQuery(prodInst);
            print.addAttribute(CIProducts.ProductAbstract.Name);
            print.executeWithoutAccessCheck();

            final Insert insert = new Insert(CIProducts.TreeViewProduct);
            insert.add(CIProducts.TreeViewProduct.ParentLink, parentInst);
            insert.add(CIProducts.TreeViewProduct.ProductLink, prodInst);
            insert.add(CIProducts.TreeViewProduct.Label, print.<String>getAttribute(CIProducts.ProductAbstract.Name));
            insert.execute();
        }
        return new Return();
    }

    /**
     * Auto complete4 tree view.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return autoComplete4TreeView(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<String, Map<String, String>>();

        final String key = containsProperty(_parameter, "Key") ? getProperty(_parameter, "Key") : "OID";

        final QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
        queryBldr.addWhereAttrMatchValue(CIProducts.TreeViewAbstract.Label, input + "*").setIgnoreCase(true);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.TreeViewAbstract.Label);
        multi.addAttribute(key);
        multi.execute();
        while (multi.next()) {
            final String label = multi.<String>getAttribute(CIProducts.TreeViewAbstract.Label);
            final Map<String, String> map = new HashMap<String, String>();
            map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), multi.getAttribute(key).toString());
            map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), label);
            map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), label);
            orderMap.put(label, map);
        }
        list.addAll(orderMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Gets the tree view label.
     *
     * @param _parameter the _parameter
     * @param _treeViewInst the _tree view inst
     * @param _productInst the _product inst
     * @return the tree view label
     * @throws EFapsException the e faps exception
     */
    protected static String getTreeViewLabel(final Parameter _parameter,
                                             final Instance _treeViewInst,
                                             final Instance _productInst)
        throws EFapsException
    {
        final boolean hideRoot = Boolean.parseBoolean(new TreeView().getProperty(_parameter, "HideRoot", "false"));
        final boolean hideProduct = Boolean.parseBoolean(new TreeView().getProperty(_parameter, "HideProduct", "false"));
        return TreeView.getTreeViewLabel(_parameter, _treeViewInst, _productInst, hideRoot, hideProduct);
    }

    /**
     * Gets the tree view label.
     *
     * @param _parameter the _parameter
     * @param _treeViewInst the _tree view inst
     * @param _productInst the _product inst
     * @param _hideRoot the _hide root
     * @param _hideProduct the _hide product
     * @return the tree view label
     * @throws EFapsException the e faps exception
     */
    protected static String getTreeViewLabel(final Parameter _parameter,
                                             final Instance _treeViewInst,
                                             final Instance _productInst,
                                             final boolean _hideRoot,
                                             final boolean _hideProduct)
        throws EFapsException
    {
        final StringBuilder ret = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.TreeViewProduct);
        queryBldr.addWhereAttrEqValue(CIProducts.TreeViewProduct.ProductLink, _productInst);
        final CachedInstanceQuery query = queryBldr.getCachedQuery(TreeView.CACHEKEY)
                        .setLifespan(30).setLifespanUnit(TimeUnit.MINUTES);
        query.execute();
        while (query.next()) {
            final Map<Instance, String> labelmap = getLabelMap(_parameter, query.getCurrentValue());
            if (labelmap.keySet().contains(_treeViewInst)) {
                for (final Entry<Instance, String> entry : labelmap.entrySet()) {
                    if (entry.getKey().getType().isCIType(CIProducts.TreeViewRoot) && !_hideRoot
                                    || entry.getKey().getType().isCIType(CIProducts.TreeViewProduct) && ! _hideProduct
                                    || entry.getKey().getType().isCIType(CIProducts.TreeViewNode)) {
                        if (ret.length() > 0) {
                            ret.insert(0, " - ");
                        }
                        ret.insert(0, entry.getValue());
                    }
                }
                break;
            }
        }
        return ret.length() == 0 ? null : ret.toString();
    }


    /**
     * Gets the label map.
     *
     * @param _parameter the _parameter
     * @param _treeViewInst the _tree view inst
     * @return the label map
     * @throws EFapsException the e faps exception
     */
    protected static Map<Instance, String> getLabelMap(final Parameter _parameter,
                                                       final Instance _treeViewInst)
        throws EFapsException
    {
        final Map<Instance, String> ret = new LinkedHashMap<>();
        final PrintQuery print = new CachedPrintQuery(_treeViewInst, TreeView.CACHEKEY)
                        .setLifespan(30).setLifespanUnit(TimeUnit.MINUTES);
        final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIProducts.TreeViewAbstract.AbstractParentLink)
                        .instance();
        print.addSelect(selParentInst);
        print.addAttribute(CIProducts.TreeViewProduct.Label);
        print.execute();
        ret.put(_treeViewInst, print.<String>getAttribute(CIProducts.TreeViewProduct.Label));
        final Instance parentInst = print.getSelect(selParentInst);
        if (parentInst != null && parentInst.isValid()) {
            ret.putAll(getLabelMap(_parameter, parentInst));
        }
        return ret;
    }
}
