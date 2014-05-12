/*
 * Copyright 2003 - 2013 The eFaps Team
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

package org.efaps.esjp.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang3.StringEscapeUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.AttributeType;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.attributetype.CompanyLinkType;
import org.efaps.admin.datamodel.attributetype.CreatedType;
import org.efaps.admin.datamodel.attributetype.CreatorLinkType;
import org.efaps.admin.datamodel.attributetype.ModifiedType;
import org.efaps.admin.datamodel.attributetype.ModifierLinkType;
import org.efaps.admin.datamodel.attributetype.OIDType;
import org.efaps.admin.datamodel.attributetype.TypeType;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.admin.datamodel.RangesValue;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.ProductsSettings;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("5c2c078f-852d-49d9-af34-4ff5022b6f82")
@EFapsRevision("$Rev$")
public abstract class Product_Base
    extends CommonDocument
{
    /**
     * CacheKey for ExchangeRates.
     */
    public static String CACHEKEY4PRODUCT = Product.class.getName()+ ".CacheKey4Product";


    /**
     * @param _parameter _parameter
     * @return rangevalue
     * @throws EFapsException on error
     */
    public Return dimensionRangeValue(final Parameter _parameter)
        throws EFapsException
    {
        final RangesValue rval = new RangesValue()
        {
            @Override
            protected void setSelectedValue(final Parameter _parameter,
                                            final Map<?, ?> _valueMap)
                throws EFapsException
            {
                final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
                final Instance dimInst = Products.getSysConfig().getLink(ProductsSettings.DEFAULTDIMENSION);
                if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.CREATE)
                                && dimInst != null && dimInst.isValid()) {
                    for (final Entry<?, ?> entry : _valueMap.entrySet()) {
                        if (entry.getValue().equals(Long.valueOf(dimInst.getId()).toString())) {
                            fieldValue.setValue(entry.getKey());
                            break;
                        }
                    }
                }
            }
        };
        return rval.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return dafuelt value
     * @throws EFapsException
     */
    public Return defaultUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Field field = new Field();
        final Instance dimInst = Products.getSysConfig().getLink(ProductsSettings.DEFAULTDIMENSION);
        final Return ret;
        if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.CREATE) && dimInst != null
                        && dimInst.isValid()) {
            final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
            fieldValue.setValue(Dimension.get(dimInst.getId()).getBaseUoM().getId());
            ret = field.getUoMDropDownFieldValue(_parameter);
        } else if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.EDIT)) {
            ret = field.getUoMDropDownFieldValue(_parameter);
        } else {
            ret = field.emptyDropDownFieldValue(_parameter);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return set values script in list
     * @throws EFapsException on error
     */
    public Return updateFields4Dimension(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final Collection<Map<String, String>> values = new ArrayList<Map<String, String>>();
        ret.put(ReturnValues.VALUES, values);
        final Map<String, String> map = new HashMap<String, String>();
        values.add(map);
        final Long dimId = Long.valueOf(_parameter
                        .getParameterValue(CIFormProducts.Products_ProductForm.dimension.name));
        final Dimension dim = Dimension.get(dimId);
        final StringBuilder js = new StringBuilder()
                        .append("new Array('").append(dim.getBaseUoM().getId()).append("'");
        for (final UoM uom : dim.getUoMs()) {
            js.append(",'").append(uom.getId()).append("','").append(uom.getName()).append("'");
        }
        js.append(")");
        map.put(CIFormProducts.Products_ProductForm.defaultUoM.name, js.toString());
        return ret;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _prodInstance instance of a product the batch is wanted for
     * @return Instance of the Batch Product
     * @throws EFapsException on error
     */
    public Instance createBatch(final Parameter _parameter,
                                final Instance _prodInstance)
        throws EFapsException
    {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("Name", getName4Batch(_parameter));
        final Instance batchInst = cloneProduct(_parameter, _prodInstance,
                        CIProducts.ProductBatch.getType(), map, false);
        final Insert relInsert = new Insert(CIProducts.StockProductAbstract2Batch);
        relInsert.add(CIProducts.StockProductAbstract2Batch.FromLink, _prodInstance);
        relInsert.add(CIProducts.StockProductAbstract2Batch.ToLink, batchInst);
        relInsert.execute();
        return batchInst;
    }


    protected String getName4Batch(final Parameter _parameter)
        throws EFapsException
    {
        // Products_ProductBatchSequence
        final NumberGenerator numGen = NumberGenerator.get(UUID.fromString("a1cc9de2-1f69-456c-83aa-a7c5b8f8c802"));
        final String ret = numGen.getNextVal(new DateTime().toDate());
        return ret;
    }

    /**
     * Method for create unique product.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createIndividual(final Parameter _parameter)
        throws EFapsException
    {
        final String prodOid = _parameter.getParameterValue("product");
        final String name = _parameter.getParameterValue("name");
        final String warehouse = _parameter.getParameterValue("storage");

        if (prodOid != null && prodOid.length() > 0) {
            final Instance instance = Instance.get(prodOid);
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put("Name", name);
            final Instance uniqueInst = cloneProduct(_parameter, instance,
                            CIProducts.ProductIndividual.getType(), map, true);

            final PrintQuery print = new PrintQuery(uniqueInst);
            print.addAttribute("Dimension");
            print.execute();

            final Insert insert = new Insert(CIProducts.TransactionInbound);
            insert.add("Quantity", 1);
            insert.add("UoM", Dimension.get(print.<Long>getAttribute("Dimension")).getBaseUoM().getId());
            insert.add("Storage", warehouse);
            insert.add("Product", uniqueInst.getId());
            insert.add("Description", DBProperties.getProperty("org.efaps.esjp.products.Product.createUnique"));
            insert.add("Date", new DateTime());
            insert.execute();

            final Insert insert2 = new Insert(CIProducts.TransactionOutbound);
            insert2.add("Quantity", 1);
            insert2.add("UoM", Dimension.get(print.<Long>getAttribute("Dimension")).getBaseUoM().getId());
            insert2.add("Storage", warehouse);
            insert2.add("Product", instance.getId());
            insert2.add("Description", DBProperties.getProperty("org.efaps.esjp.products.Product.createUnique"));
            insert2.add("Date", new DateTime());
            insert2.execute();
        }
        return new Return();
    }

    /**
     * Method is used for an autocomplete field in the form for transactions.
     *
     * @param _parameter parameter from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<String, Map<String, String>>();
        if (!input.isEmpty()) {
            boolean cache = true;
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            QueryBuilder queryBldr = getQueryBldrFromProperties(_parameter);
            if (queryBldr == null) {
                queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            }
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*");
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Name);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*")
                                .setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Description);
            }

            if (!"true".equalsIgnoreCase(getProperty(_parameter, "IncludeInActive"))) {
                queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);
            }

            final Map<Integer, String> classes = analyseProperty(_parameter, "Classification");
            if (!classes.isEmpty()) {
                final List<Classification> classTypes = new ArrayList<Classification>();
                for (final String clazz : classes.values()) {
                    classTypes.add((Classification) Type.get(clazz));
                }
                queryBldr.addWhereClassification(classTypes.toArray(new Classification[classTypes.size()]));
            }

            if (containsProperty(_parameter, "InStock")) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                final String[] storageArr = _parameter.getParameterValues("storage");
                if (storageArr != null && storageArr.length > 0) {
                    final int selected = getSelectedRow(_parameter);
                    final Instance storInst = Instance.get(storageArr[selected]);
                    if (storInst.isValid()) {
                        attrQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storInst);
                    }
                }
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIProducts.Inventory.Product);
                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID, attrQuery);
                cache = false;
            }

            final Map<Integer, String> excludes = analyseProperty(_parameter, "ExcludeType");
            for (final String element : excludes.values()) {
                final QueryBuilder queryBldr2 = new QueryBuilder(Type.get(element));
                final AttributeQuery attrQuery = queryBldr2.getAttributeQuery(CIProducts.ProductAbstract.ID);
                queryBldr.addWhereAttrNotInQuery(CIProducts.ProductAbstract.ID, attrQuery);
            }

            InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);

            cache = cache && add2QueryBldr4autoComplete4Product(_parameter, queryBldr);

            final MultiPrintQuery multi = cache ? queryBldr.getCachedPrint(Product_Base.CACHEKEY4PRODUCT)
                            : queryBldr.getPrint();
            multi.addAttribute(CIProducts.ProductAbstract.OID, CIProducts.ProductAbstract.Name,
                            CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Dimension);
            multi.execute();

            while (multi.next()) {
                final String name = (String) multi.getAttribute(CIProducts.ProductAbstract.Name);
                final String desc = (String) multi.getAttribute(CIProducts.ProductAbstract.Description);
                final String oid = (String) multi.getAttribute(CIProducts.ProductAbstract.OID);
                final String choice = nameSearch ? name + " - " + desc : desc + " - " + name;
                final Map<String, String> map = new HashMap<String, String>();
                map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), oid);
                map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
                orderMap.put(choice, map);
            }
            list.addAll(orderMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _queryBldr    QueryBuilder to add to
     * @return true if allow cache, else false
     * @throws EFapsException on error
     */
    protected boolean add2QueryBldr4autoComplete4Product(final Parameter _parameter,
                                                         final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be used from implementation
        return true;
    }


    public Return productMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint() {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                // if products with serialnumber or batches is activated validate if they must be shown or not
                if (Products.getSysConfig().getAttributeValueAsBoolean(ProductsSettings.ACTIVATEINDIVIDUAL)) {
                    if (!"true".equalsIgnoreCase(getProperty(_parameter, "IncludeIndividual"))) {
                        _queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type,
                                        CIProducts.ProductIndividual.getType().getId(),
                                        CIProducts.ProductBatch.getType().getId());
                    }
                }
            }
        };
        return multi.execute(_parameter);
    }

    public Return updateFields4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final Map<String, Object> map = new HashMap<String, Object>();

        final int selected = getSelectedRow(_parameter);
        final String prodOid = _parameter.getParameterValues("product")[selected];
        // validate that a product was selected
        if (prodOid.length() > 0) {
            final PrintQuery print = new PrintQuery(prodOid);
            print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                            CIProducts.ProductAbstract.Dimension, CIProducts.ProductAbstract.DefaultUoM);
            print.execute();
            map.put("productAutoComplete", print.getAttribute(CIProducts.ProductAbstract.Name));
            map.put("productDesc", print.getAttribute(CIProducts.ProductAbstract.Description));
            final Long dimId = print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension);
            final Long dUoMId = print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM);
            long selectedUoM;
            if (dUoMId == null) {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            } else {
                if (Dimension.getUoM(dUoMId).getDimension().equals(Dimension.get(dimId))) {
                    selectedUoM = dUoMId;
                } else {
                    selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
                }
            }
            map.put("uoM", getUoMFieldStr(selectedUoM, dimId));
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            map.put("productAutoComplete", "");
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        }
        return retVal;
    }

    /**
     * Method to evaluate the selected row.
     *
     * @param _parameter paaremter
     * @return number of selected row.
     */
    protected int getSelectedRow(final Parameter _parameter)
    {
        int ret = 0;
        final String value = _parameter.getParameterValue("eFapsRowSelectedRow");
        if (value != null && value.length() > 0) {
            ret = Integer.parseInt(value);
        }
        return ret;
    }

    /**
     * Method JavaScript for create unique field value.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return ret with parameter.
     * @throws EFapsException on error.
     */
    public Return javascript4CreateUniqueFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String selected = _parameter.getParameterValue("selectedRow");
        final StringBuilder js = new StringBuilder();
        if (selected != null && selected.length() > 0) {
            final PrintQuery print = new PrintQuery(selected);
            print.addAttribute("Name");
            print.execute();
            js.append("<script type=\"text/javascript\">")
                .append("Wicket.Event.add(window, \"domready\", function(event) {")
                .append("document.getElementsByName('product')[0].value='").append(selected).append("';")
                .append("document.getElementsByName('productAutoComplete')[0].value='")
                .append(StringEscapeUtils.escapeEcmaScript((String) print.getAttribute("Name"))).append("';")
                .append("});")
                .append("</script>");
        } else {
            js.append("<script type=\"text/javascript\">")
                .append("Wicket.Event.add(window, \"domready\", function(event) {")
                .append("inputs = document.getElementsByTagName('INPUT');")
                .append("for (i=0;i<inputs.length;i++) {")
                .append("inputs[i].blur();")
                .append(" });")
                .append("</script>");
        }
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    public Return autoComplete4MassiveProductsInStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Instance storage = (Instance) Context.getThreadContext()
                        .getSessionAttribute(Transaction_Base.STORAGEINSTKEY);
        final Instance instance = _parameter.getInstance();
        if ((storage != null && storage.isValid()) && instance == null) {
            _parameter.put(ParameterValues.INSTANCE, storage);
        }
        return autoComplete4ProductsInStorage(_parameter);
    }

    /**
     * Method is used for an autocomplete field in the move form.
     *
     * @param _parameter parameter from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return autoComplete4ProductsInStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final Set<String> products = new HashSet<String>();
        final QueryBuilder invQueryBldr = new QueryBuilder(CIProducts.Inventory);
        invQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, instance.getId());
        final MultiPrintQuery invMulti = invQueryBldr.getPrint();
        invMulti.addAttribute(CIProducts.Inventory.Product, CIProducts.Inventory.Quantity);
        invMulti.execute();
        while (invMulti.next()) {
            final BigDecimal quantity = invMulti.<BigDecimal>getAttribute(CIProducts.Inventory.Quantity);
            if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                products.add(invMulti.<Long>getAttribute(CIProducts.Inventory.Product).toString());
            }
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        if (input.length() > 0) {
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*").setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Name);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*").setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Description);
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.ProductAbstract.ID, CIProducts.ProductAbstract.Name,
                               CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Dimension);
            multi.execute();
            while (multi.next()) {
                final Long id = multi.<Long>getAttribute(CIProducts.ProductAbstract.ID);
                if (products.contains(id.toString())) {
                    final String name = multi.<String>getAttribute(CIProducts.ProductAbstract.Name);
                    final String desc = multi.<String>getAttribute(CIProducts.ProductAbstract.Description);
                    final String choice = nameSearch ? name + " - " + desc : desc + " - " + name;
                    final Map<String, String> map = new HashMap<String, String>();
                    map.put(EFapsKey.AUTOCOMPLETE_KEY.getKey(), id.toString());
                    map.put(EFapsKey.AUTOCOMPLETE_VALUE.getKey(), name);
                    map.put(EFapsKey.AUTOCOMPLETE_CHOICE.getKey(), choice);
                    map.put("uoM", getUoMFieldStr(multi.<Long>getAttribute(CIProducts.ProductAbstract.Dimension)));
                    list.add(map);
                }
            }
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method to create a STring for UoM fields.
     *
     * @param _dimId id of the dimension the UoM is wanted for
     * @return String
     */
    @Override
    protected String getUoMFieldStr(final long _dimId)
        throws CacheReloadException
    {
        final Dimension dim = Dimension.get(_dimId);
        final StringBuilder js = new StringBuilder();
        js.append("new Array('").append(dim.getBaseUoM().getId()).append("'");
        for (final UoM uom : dim.getUoMs()) {
            js.append(",'").append(uom.getId()).append("','").append(uom.getName()).append("'");
        }
        js.append(")");
        return js.toString();
    }

    /**
     * Method is used to make a clone of a product into another product. e.g.
     * from a standard product into a unique product.
     *
     * @param _instance instance to be cloned
     * @param _cloneType type of the cloned product
     * @param _attrMap map of attributes that will be set instead of the value
     *            from the original
     * @return instance of the new product
     * @throws EFapsException on error
     */
    public Instance cloneProduct(final Parameter _parameter,
                                 final Instance _instance,
                                 final Type _cloneType,
                                 final Map<String, Object> _attrMap,
                                 final boolean _addClassifications)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        for (final Attribute attr : _instance.getType().getAttributes().values()) {
            print.addAttribute(attr);
        }
        print.execute();

        final Set<String> addedAttributes = new HashSet<String>();

        final Insert insert = new Insert(_cloneType);
        for (final Attribute attr : _instance.getType().getAttributes().values()) {
            // TODO that must be done by type or??
            if (addAttribute(attr, addedAttributes) && _cloneType.getAttributes().containsKey(attr.getName())) {
                insert.add(_cloneType.getAttribute(attr.getName()), _attrMap.containsKey(attr.getName())
                                    ? _attrMap.get(attr.getName())
                                    : print.getAttribute(attr));
            }
        }
        insert.execute();
        final Instance ret = insert.getInstance();
        if (_addClassifications) {
            final Set<Classification> classTypes = new HashSet<Classification>();
            Type curr = _instance.getType();
            while (curr.getParentType() != null) {
                classTypes.addAll(curr.getClassifiedByTypes());
                curr = curr.getParentType();
            }
            classTypes.addAll(curr.getClassifiedByTypes());
            final Set<String> oids = new HashSet<String>();
            for (final Classification classType : classTypes) {
                final QueryBuilder relQueryBldr = new QueryBuilder(classType.getClassifyRelationType());
                relQueryBldr.addWhereAttrEqValue(classType.getRelLinkAttributeName(), _instance.getId());
                final MultiPrintQuery relMulti = relQueryBldr.getPrint();
                relMulti.addAttribute(classType.getRelTypeAttributeName());
                relMulti.execute();

                while (relMulti.next()) {
                    final String oid = relMulti.getCurrentInstance().getOid();
                    if (!oids.contains(oid)) {
                        oids.add(oid);
                        final Long typeid = relMulti.<Long>getAttribute(classType.getRelTypeAttributeName());
                        final Classification subClassType = (Classification) Type.get(typeid);

                        final Insert relInsert = new Insert(classType.getClassifyRelationType());
                        relInsert.add(classType.getRelLinkAttributeName(), ret.getId());
                        relInsert.add(classType.getRelTypeAttributeName(), typeid);
                        relInsert.execute();

                        final QueryBuilder queryBldr = new QueryBuilder(subClassType);
                        queryBldr.addWhereAttrEqValue(subClassType.getLinkAttributeName(), _instance.getId());
                        final MultiPrintQuery multi = queryBldr.getPrint();
                        for (final Attribute attr : subClassType.getAttributes().values()) {
                            multi.addAttribute(attr);
                        }
                        multi.execute();
                        while (multi.next()) {
                            final Insert subInsert = new Insert(subClassType);
                            subInsert.add(subClassType.getLinkAttributeName(), ret.getId());
                            for (final Attribute attr : subClassType.getAttributes().values()) {
                                if (addAttribute(attr, addedAttributes)
                                                && !subClassType.getLinkAttributeName().equals(attr.getName())) {
                                    final Object object = multi.getAttribute(attr);
                                    if (object instanceof Object[]) {
                                        subInsert.add(attr, (Object[]) object);
                                    } else {
                                        subInsert.add(attr, object);
                                    }
                                }
                            }
                            subInsert.execute();
                        }
                    }
                }
            }
        }
        createRelation(_parameter, _instance, ret);

        return ret;
    }

    protected void createRelation(final Parameter _parameter,
                                  final Instance _parent,
                                  final Instance _child)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        if (properties.containsKey("ConnectType")) {
            final String childAttr = (String) properties.get("ConnectChildAttribute");
            final String parentAttr = (String) properties.get("ConnectParentAttribute");
            final Type type = Type.get((String) properties.get("ConnectType"));

            // make the relation between original and copy
            final Insert relInsert = new Insert(type);
            relInsert.add(parentAttr, _parent);
            relInsert.add(childAttr, _child);
            relInsert.execute();
        }
    }

    /**
     * method for add attribute in case not exists.
     *
     * @param _attr Attribute of the table name.
     * @param _addedAttributes Set&lt;String&gt; for adding attributes.
     * @return ret Return.
     */
    private boolean addAttribute(final Attribute _attr,
                                 final Set<String> _addedAttributes)
    {
        boolean ret = false;
        final String key = (_attr.getTable() != null ? _attr.getTable().getName() : _attr.getName())
                        + _attr.getSqlColNames();
        if (!_addedAttributes.contains(key)) {
            _addedAttributes.add(key);
            final AttributeType attrType = _attr.getAttributeType();
            ret = !attrType.getClassRepr().equals(CompanyLinkType.class)
                            && !attrType.getClassRepr().equals(CreatedType.class)
                            && !attrType.getClassRepr().equals(CreatorLinkType.class)
                            && !attrType.getClassRepr().equals(ModifiedType.class)
                            && !attrType.getClassRepr().equals(ModifierLinkType.class)
                            && !attrType.getClassRepr().equals(OIDType.class)
                            && !attrType.getClassRepr().equals(TypeType.class) && !"ID".equals(_attr.getName());
        }
        return ret;
    }

    /**
     * create a new select with values the storage.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return retVal with JavaScript.
     * @throws EFapsException on error.
     */
    public Return storageFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
        final TreeMap<String, Long> map = new TreeMap<String, Long>();
        long actual = 0;

        //Sales_Configuration
        final SystemConfiguration config = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f"));
        if (config != null) {
            final Instance inst = config.getLink("DefaultWarehouse");
            actual = inst.getId();
        }

        final QueryBuilder querybldr = new QueryBuilder(CIProducts.StorageAbstract);
        final MultiPrintQuery multi = querybldr.getPrint();
        multi.addAttribute(CIProducts.StorageAbstract.ID, CIProducts.StorageAbstract.Name);
        multi.execute();
        while (multi.next()) {
            map.put(multi.<String>getAttribute(CIProducts.StorageAbstract.Name)
                            , multi.<Long>getAttribute(CIProducts.StorageAbstract.ID));
        }

        final StringBuilder ret = new StringBuilder();

        ret.append("<select size=\"1\" name=\"").append(fieldValue.getField().getName()).append("\">");
        for (final Map.Entry<String, Long> entry : map.entrySet()) {
            ret.append("<option");
            if (entry.getValue().equals(actual)) {
                ret.append(" selected=\"selected\" ");
            }
            ret.append(" value=\"").append(entry.getValue()).append("\">").append(entry.getKey())
               .append("</option>");
        }
        ret.append("</select>");
        retVal.put(ReturnValues.SNIPLETT, ret.toString());
        return retVal;
    }

    /**
     * Executed on validate event to check the information for a new product.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return containing true if valid
     * @throws EFapsException on error
     */
    public Return validateProductName(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String name = _parameter.getParameterValue("name");
        final StringBuilder warnHtml = validateName4Product(_parameter, name);
        if (!warnHtml.toString().isEmpty()) {
            ret.put(ReturnValues.SNIPLETT, warnHtml.toString());
        } else {
            ret.put(ReturnValues.TRUE, true);
        }

        return ret;
    }

    /**
     * Method for return the name of a product.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _html StringBuilder to append to
     * @param _name String
     * @return StringBuilder with html.
     * @throws EFapsException on error
     */
    protected StringBuilder validateName4Product(final Parameter _parameter,
                                                 final String _name)
        throws EFapsException
    {
        final StringBuilder html = new StringBuilder();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductStandart);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductStandart.Name, _name).setIgnoreCase(true);
        final InstanceQuery query = queryBldr.getQuery();
        if (!query.execute().isEmpty()) {
            html.append("<div style=\"text-align:center;\">")
                            .append(DBProperties.getProperty("org.efaps.esjp.products.Product.existingProduct"))
                            .append("</div>");
        }
        return html;
    }


    protected List<BOMCalculator> getCalulators4Product(final Parameter _parameter,
                                                        final Instance _prodInst)
        throws EFapsException
    {
        final List<BOMCalculator> ret = new ArrayList<BOMCalculator>();
        final QueryBuilder qlb = new QueryBuilder(CIProducts.BOMAbstract);
        qlb.addWhereAttrEqValue(CIProducts.BOMAbstract.FromAbstract, _prodInst);
        final MultiPrintQuery multi = qlb.getPrint();
        multi.addAttribute(CIProducts.BOMAbstract.Quantity);
        final SelectBuilder select = new SelectBuilder().linkto(CIProducts.BOMAbstract.ToAbstract).instance();
        multi.addSelect(select);
        multi.execute();
        while(multi.next()){
            final Instance instBOMproduct = multi.<Instance>getSelect(select);
            final BigDecimal quantityRequired = multi.getAttribute(CIProducts.BOMAbstract.Quantity);
            final BOMCalculator calBOM= new BOMCalculator(_parameter, instBOMproduct, quantityRequired);
            ret.add(calBOM);
        }

        return ret;

    }

    public int getQuantity2ProductBOM(final Parameter _parameter,
                                   final Instance _prodInst)
        throws EFapsException
    {
        int productQuantity = 0;
        final List<BOMCalculator> boms = getCalulators4Product(_parameter, _prodInst);
        int i=0;
        boolean band=true;
        boolean first =true;
        while (i < boms.size() && band) {
            final BOMCalculator bom = boms.get(i);
            final int bomquantity = bom.getFactory();
            if (bomquantity == 0) {
                productQuantity = 0;
                band = false;
            } else if (first) {
                first = false;
                productQuantity = bomquantity;
            } else if (bomquantity < productQuantity) {
                productQuantity = bomquantity;
            }
            i++;
        }
        return productQuantity;
    }

    public Return getCosting4Product(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {
            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.TransactionInOutAbstract);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.TransactionInOutAbstract.Product,
                                _parameter.getInstance());
                final AttributeQuery attrQuery = attrQueryBldr
                                .getAttributeQuery(CIProducts.TransactionInOutAbstract.ID);
                _queryBldr.addWhereAttrInQuery(CIProducts.Costing.TransactionAbstractLink, attrQuery);
                super.add2QueryBldr(_parameter, _queryBldr);
            }
        };
        return multi.execute(_parameter);
    }

}
