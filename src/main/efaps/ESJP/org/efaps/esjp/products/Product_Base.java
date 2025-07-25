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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.StringSubstitutor;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.AttributeSet;
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
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.datamodel.ui.LinkWithRangesUI;
import org.efaps.admin.datamodel.ui.UIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.Form;
import org.efaps.api.ui.IFilter;
import org.efaps.api.ui.IFilterList;
import org.efaps.api.ui.IMapFilter;
import org.efaps.api.ui.IOption;
import org.efaps.db.AttributeQuery;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Delete;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Query;
import org.efaps.eql.builder.Selectables;
import org.efaps.eql2.bldr.AbstractQueryEQLBuilder;
import org.efaps.esjp.admin.datamodel.RangesValue;
import org.efaps.esjp.ci.CIFormProducts;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.AbstractWarning;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.esjp.erp.IWarning;
import org.efaps.esjp.erp.WarningUtil;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.ui.rest.provider.ITableProvider;
import org.efaps.esjp.ui.rest.provider.StandardTableProvider;
import org.efaps.ui.wicket.models.objects.UIForm;
import org.efaps.ui.wicket.models.objects.UIForm.ElementType;
import org.efaps.ui.wicket.models.objects.UITable;
import org.efaps.ui.wicket.models.objects.UITable.TableFilter;
import org.efaps.ui.wicket.models.objects.grid.CacheKey;
import org.efaps.ui.wicket.models.objects.grid.GridRow;
import org.efaps.ui.wicket.models.objects.grid.UIFieldGrid;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("5c2c078f-852d-49d9-af34-4ff5022b6f82")
@EFapsApplication("eFapsApp-Products")
public abstract class Product_Base
    extends CommonDocument
    implements ITableProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(Product.class);

    /**
     * CacheKey for ExchangeRates.
     */
    public static final String CACHEKEY4PRODUCT = Product.class.getName() + ".CacheKey4Product";


    private ITableProvider tableProvider;
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

            /** */
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean isSelected(final Parameter _parameter,
                                         final RangeValueOption _option)
                throws EFapsException
            {
                boolean ret = false;
                final Instance dimInst = Products.DEFAULTDIMENSION.get();
                if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.CREATE) && dimInst != null && dimInst
                                .isValid()) {
                    ret = dimInst.getId() == (Long) _option.getValue();
                }
                return ret;
            }
        };
        return rval.execute(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return dafuelt value
     * @throws EFapsException on error
     */
    public Return defaultUoMFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Field field = new Field();
        final Instance dimInst = Products.DEFAULTDIMENSION.get();
        final Return ret;
        if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.CREATE) && dimInst != null
                        && dimInst.isValid()) {
            ParameterUtil.setProperty(_parameter, "Dimension", Dimension.get(dimInst.getId()).getName());
            ret = field.getUoMDropDownFieldValue(_parameter);
        } else if (_parameter.get(ParameterValues.ACCESSMODE).equals(TargetMode.EDIT)) {
            ret = field.getUoMDropDownFieldValue(_parameter);
        } else {
            ret = new Return();
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

        final Collection<Map<String, String>> values = new ArrayList<>();
        ret.put(ReturnValues.VALUES, values);
        final Map<String, String> map = new HashMap<>();
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
     * Creates the Product.
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
                add2Create(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Add to create.
     *
     * @param _parameter the _parameter
     * @param _insert the _insert
     * @throws EFapsException on error
     */
    protected void add2Create(final Parameter _parameter,
                              final Insert _insert)
        throws EFapsException
    {
        if (isFamilyActivated(_parameter, _insert.getInstance())) {
            final Instance famInst = Instance.get(_parameter
                            .getParameterValue(CIFormProducts.Products_ProductForm.productFamilyLink.name));
            if (famInst.isValid()) {
                _insert.add(CIProducts.ProductAbstract.ProductFamilyLink, famInst);
                _insert.add(CIProducts.ProductAbstract.Name, getNameFromUI(_parameter, _insert.getInstance()));
            }
        }
    }

    /**
     * Checks if is family activated.
     *
     * @param _parameter the _parameter
     * @param _instance the _instance
     * @return true, if is family activated
     * @throws EFapsException on error
     */
    protected boolean isFamilyActivated(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {
        boolean ret = false;
        Instance instance = _instance;
        if (instance == null) {
            final Object obj = _parameter.get(ParameterValues.UIOBJECT);
            if (obj instanceof AbstractCommand) {
                final Type type = ((AbstractCommand) obj).getTargetCreateType();
                instance = Instance.get(type, 0);
            }
        }

        if (instance != null && instance.getType() != null) {
            if (instance.getType().isCIType(CIProducts.ProductMaterial)) {
                ret = Products.MATERIAL_ACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductStandart)) {
                ret = Products.STANDART_ACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductGeneric)) {
                ret = Products.GENERICACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductService)) {
                ret = Products.SERV_ACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductVariantBase)) {
                ret = Products.VARIANTACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductInfinite)) {
                ret = Products.INFINITEACTFAM.get();
            } else if (instance.getType().isCIType(CIProducts.ProductSalesPartList)) {
                ret = Products.SALESPARTLIST_ACTFAM.get();
            }
        }
        return ret;
    }

    /**
     * Edits the.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
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
                add2Edit(_parameter, _update);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Add to edit.
     *
     * @param _parameter the _parameter
     * @param _update the _update
     * @throws EFapsException on error
     */
    protected void add2Edit(final Parameter _parameter,
                            final Update _update)
        throws EFapsException
    {
        if (isFamilyActivated(_parameter, _update.getInstance())) {
            final Instance famInst = Instance.get(_parameter
                            .getParameterValue(CIFormProducts.Products_ProductForm.productFamilyLink.name));
            if (famInst.isValid()) {
                _update.add(CIProducts.ProductAbstract.ProductFamilyLink, famInst);
            }
            _update.add(CIProducts.ProductAbstract.Name, getNameFromUI(_parameter, _update.getInstance()));
        }
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _prodInstance instance of a product the batch is wanted for
     * @return Instance of the Batch Product
     * @throws EFapsException on error
     */
    public Instance createBatch(final Parameter _parameter,
                                final Instance _prodInstance)
        throws EFapsException
    {
        final Map<String, Object> map = new HashMap<>();
        map.put("Name", getName4Batch(_parameter));
        final Instance batchInst = cloneProduct(_parameter, _prodInstance,
                        CIProducts.ProductBatch.getType(), map, false);
        final Insert relInsert = new Insert(CIProducts.StoreableProductAbstract2Batch);
        relInsert.add(CIProducts.StoreableProductAbstract2Batch.FromLink, _prodInstance);
        relInsert.add(CIProducts.StoreableProductAbstract2Batch.ToLink, batchInst);
        relInsert.execute();
        return batchInst;
    }

    /**
     * Gets the name for a batch.
     *
     * @param _parameter the _parameter
     * @return the name4 batch
     * @throws EFapsException on error
     */
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
            final Map<String, Object> map = new HashMap<>();
            map.put(CIProducts.ProductIndividual.Individual.name, ProductIndividual.NONE.getInt());
            map.put(CIProducts.ProductIndividual.Name.name, name);
            final Instance uniqueInst = cloneProduct(_parameter, instance,
                            CIProducts.ProductIndividual.getType(), map, true);

            final PrintQuery print = new PrintQuery(uniqueInst);
            print.addAttribute(CIProducts.ProductIndividual.Dimension);
            print.execute();

            final Insert insert = new Insert(CIProducts.TransactionIndividualInbound);
            insert.add(CIProducts.TransactionIndividualInbound.Quantity, 1);
            insert.add(CIProducts.TransactionIndividualInbound.UoM,
                            Dimension.get(print.<Long>getAttribute(CIProducts.ProductIndividual.Dimension))
                                            .getBaseUoM());
            insert.add(CIProducts.TransactionIndividualInbound.Storage, warehouse);
            insert.add(CIProducts.TransactionIndividualInbound.Product, uniqueInst.getId());
            insert.add(CIProducts.TransactionIndividualInbound.Description,
                            DBProperties.getProperty(Product.class.getName() + ".createUnique"));
            insert.add(CIProducts.TransactionIndividualInbound.Date, new DateTime());
            insert.execute();
        }
        return new Return();
    }

    /**
     * Method is used as the standard autocomplete field.
     *
     * @param _parameter parameter from eFaps
     * @return Return
     * @throws EFapsException on error
     */
    public Return autoComplete4Product(final Parameter _parameter)
        throws EFapsException
    {
        if (Products.AUTOCOMPLETE.exists()) {
            return configuredAutoComplete4Product(_parameter);
        }

        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, Map<String, String>> orderMap = new TreeMap<>();
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
                final List<Classification> classTypes = new ArrayList<>();
                for (final String clazz : classes.values()) {
                    classTypes.add((Classification) Type.get(clazz));
                }
                queryBldr.addWhereClassification(classTypes.toArray(new Classification[classTypes.size()]));
            }

            // Possibility to exclude types from the search
            if (containsProperty(_parameter, "ExcludeType")) {
                final Map<Integer, String> excludes = analyseProperty(_parameter, "ExcludeType");
                final boolean first = true;
                QueryBuilder attrQueryBldr = null;
                for (final String element : excludes.values()) {
                    final Type type;
                    if (isUUID(element)) {
                        type = Type.get(UUID.fromString(element));
                    } else {
                        type = Type.get(element);
                    }
                    if (first) {
                        attrQueryBldr = new QueryBuilder(type);
                    } else {
                        attrQueryBldr.addType(type);
                    }
                }
                queryBldr.addWhereAttrNotInQuery(CIProducts.ProductAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID));
            }

            if ("true".equalsIgnoreCase(getProperty(_parameter, "InStock"))) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryAbstract);
                final String[] storageArr = _parameter.getParameterValues("storage");
                if (storageArr != null && storageArr.length > 0) {
                    final int selected = getSelectedRow(_parameter);
                    final Instance storInst = Instance.get(storageArr[selected]);
                    if (storInst.isValid()) {
                        attrQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storInst);
                    }
                } else if ("true".equalsIgnoreCase(getProperty(_parameter, "UseDefaultWareHouse"))) {
                    final Instance defaultStorageInst = Storage.getDefaultStorage(_parameter);
                    if (defaultStorageInst != null && defaultStorageInst.isValid()) {
                        attrQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, defaultStorageInst);
                    }
                }
                if ("true".equalsIgnoreCase(getProperty(_parameter, "ExcludeReservation"))) {
                    attrQueryBldr.addWhereAttrGreaterValue(CIProducts.Inventory.Quantity, 0);
                }
                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProducts.Inventory.Product));
                cache = false;
            }

            if (containsProperty(_parameter, "ContactIsSupplierProvider")) {
                final Instance contactInst = Instance.get(_parameter.getParameterValue(
                                getProperty(_parameter, "Field4Contact", "contact")));
                if (contactInst.isValid()) {
                    final QueryBuilder coQueryBldr = new QueryBuilder(CIProducts.Product2ContactAbstract);
                    coQueryBldr.addWhereAttrEqValue(CIProducts.Product2ContactAbstract.ToAbstract, contactInst);
                    queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                    coQueryBldr.getAttributeQuery(CIProducts.Product2ContactAbstract.FromAbstract));
                }
            }

            InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);

            cache = add2QueryBldr4AutoComplete4Product(_parameter, queryBldr) && cache;

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
                final Map<String, String> map = new HashMap<>();
                map.put("eFapsAutoCompleteKEY", oid);
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", choice);
                orderMap.put(choice, map);
            }
            list.addAll(orderMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return configuredAutoComplete4Product(final Parameter parameter)
        throws EFapsException
    {
        final String input = (String) parameter.get(ParameterValues.OTHERS);

        final var configProperties = Products.AUTOCOMPLETE.get();
        final var maxResult = Integer.valueOf(configProperties.getProperty("maxResult", "200"));
        final var barcodes = Boolean.valueOf(configProperties.getProperty("searchBarcodes", "false"));

        final var uiProperties = PropertiesUtil.getProperties(parameter);

        final var types = PropertiesUtil.analyseProperty(uiProperties, "Type", 0).values()
                        .stream().collect(Collectors.toList());
        if (types.isEmpty()) {
            types.add(CIProducts.ProductAbstract.getType().getName());
        }

        final var term = input.replace("*", "%") + "%";
        AbstractQueryEQLBuilder<?> storageQuery = null;
        if (Boolean.valueOf(uiProperties.getProperty("InStock", "false"))) {
            Instance storageInst = null;
            final String[] storageArr = parameter.getParameterValues("storage");
            if (storageArr != null && storageArr.length > 0) {
                final int selected = getSelectedRow(parameter);
                storageInst = Instance.get(storageArr[selected]);
            } else if (Boolean.valueOf(uiProperties.getProperty("UseDefaultWareHouse", "false"))) {
                storageInst = Storage.getDefaultStorage(parameter);
            }
            if (InstanceUtils.isKindOf(storageInst, CIProducts.StorageAbstract)) {
                storageQuery = EQL.builder()
                                .nestedQuery(CIProducts.InventoryAbstract)
                                .where()
                                .attribute(CIProducts.InventoryAbstract.Storage).eq(storageInst)
                                .up()
                                .selectable(Selectables.attribute(CIProducts.InventoryAbstract.Product));
            }
        }

        final Map<String, Map<String, String>> orderMap = new TreeMap<>();
        final var typeArr = types.toArray(String[]::new);
        orderMap.putAll(searchByName(typeArr, term, maxResult, storageQuery));
        orderMap.putAll(searchByDescription(typeArr, term, maxResult, storageQuery));
        if (barcodes) {
            orderMap.putAll(searchByBarcode(typeArr, term, maxResult, storageQuery));
        }

        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, orderMap.values()
                        .stream()
                        .limit(maxResult)
                        .collect(Collectors.toList()));
        return retVal;
    }

    public Map<String, Map<String, String>> searchByName(final String[] types,
                                                         final String searchTerm,
                                                         final int maxResult,
                                                         final AbstractQueryEQLBuilder<?> storageQuery)
        throws EFapsException
    {
        final Map<String, Map<String, String>> orderMap = new TreeMap<>();
        final var where = EQL.builder().print()
                        .query(types)
                        .where()
                        .attribute(CIProducts.ProductAbstract.Name).like(searchTerm);

        if (storageQuery != null) {
            where.and().attribute(CIProducts.ProductAbstract.ID).in(storageQuery);
        }

        final var eval = where
                        .select()
                        .oid()
                        .attribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description)
                        .limit(maxResult)
                        .evaluate();
        while (eval.next()) {
            final String name = eval.get(CIProducts.ProductAbstract.Name);
            final String description = eval.get(CIProducts.ProductAbstract.Description);
            final String choice = name + " - " + description;
            final Map<String, String> map = new HashMap<>();
            map.put("eFapsAutoCompleteKEY", eval.get(1));
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", choice);
            orderMap.put(choice, map);
        }
        return orderMap;
    }

    public Map<String, Map<String, String>> searchByDescription(final String[] types,
                                                                final String searchTerm,
                                                                final int maxResult,
                                                                final AbstractQueryEQLBuilder<?> storageQuery)
        throws EFapsException
    {
        final Map<String, Map<String, String>> orderMap = new TreeMap<>();
        final var where = EQL.builder().print()
                        .query(types)
                        .where()
                        .attribute(CIProducts.ProductAbstract.Description).ilike(searchTerm);

        if (storageQuery != null) {
            where.and().attribute(CIProducts.ProductAbstract.ID).in(storageQuery);
        }

        final var eval = where
                        .select()
                        .oid()
                        .attribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description)
                        .limit(maxResult)
                        .evaluate();

        while (eval.next()) {
            final String name = eval.get(CIProducts.ProductAbstract.Name);
            final String description = eval.get(CIProducts.ProductAbstract.Description);
            final String choice = description + " - " + name;
            final Map<String, String> map = new HashMap<>();
            map.put("eFapsAutoCompleteKEY", eval.get(1));
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", choice);
            orderMap.put(choice, map);
        }
        return orderMap;
    }

    public Map<String, Map<String, String>> searchByBarcode(final String[] types,
                                                            final String searchTerm,
                                                            final int maxResult,
                                                            final AbstractQueryEQLBuilder<?> storageQuery)
        throws EFapsException
    {

        final Map<String, Map<String, String>> orderMap = new TreeMap<>();

        final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                        CIProducts.ProductAbstract.Barcodes.name);

        final var where = EQL.builder().print()
                        .query(attrSet.getUUID().toString())
                        .where()
                        .attribute("Code").like(searchTerm);

        if (storageQuery != null) {
            where.and().attribute("Barcodes").in(storageQuery);
        }

        final var eval = where
                        .select()
                        .attribute("Code").as("code")
                        .linkto("Barcodes").oid().as("productOid")
                        .linkto("Barcodes").attribute(CIProducts.ProductAbstract.Name).as("productName")
                        .linkto("Barcodes").attribute(CIProducts.ProductAbstract.Description).as("productDescription")
                        .limit(maxResult)
                        .evaluate();
        while (eval.next()) {
            final String code = eval.get("code");
            final String name = eval.get("productName");
            final String description = eval.get("productDescription");
            final String choice = code + ": " + name + " - " + description;
            final Map<String, String> map = new HashMap<>();
            map.put("eFapsAutoCompleteKEY", eval.get("productOid"));
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", choice);
            orderMap.put(choice, map);
        }
        return orderMap;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _queryBldr QueryBuilder to add to
     * @return true if allow cache, else false
     * @throws EFapsException on error
     */
    protected boolean add2QueryBldr4AutoComplete4Product(final Parameter _parameter,
                                                         final QueryBuilder _queryBldr)
        throws EFapsException
    {
        // to be used from implementation
        return true;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return with map for picker
     * @throws EFapsException on error
     */
    public Return picker4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final Map<String, Object> map = new HashMap<>();
        retVal.put(ReturnValues.VALUES, map);

        final Instance prodInst = Instance.get(_parameter.getParameterValue("selectedRow"));
        if (prodInst.isValid()) {
            final PrintQuery print = new PrintQuery(prodInst);
            print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                            CIProducts.ProductAbstract.DefaultUoM, CIProducts.ProductAbstract.Dimension);
            if (print.execute()) {
                final String name = print.getAttribute(CIProducts.ProductAbstract.Name);
                final String desc = print.getAttribute(CIProducts.ProductAbstract.Description);
                final Dimension dim = Dimension.get(print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension));
                final Long defUoM = print.getAttribute(CIProducts.ProductAbstract.DefaultUoM);

                map.put("eFapsPickerValue4Field", name);
                map.put("product", prodInst.getOid());
                map.put("productDesc", desc);
                map.put("uoM", getUoMFieldStr(defUoM == null ? dim.getBaseUoM().getId() : defUoM, dim.getId()));
                if (prodInst.getType().isKindOf(CIProducts.ProductIndividualAbstract)) {
                    map.put("quantity", "1");
                }
            }
        }
        return retVal;
    }

    /**
     * Product multi print.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return productMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                // if products with serialnumber or batches is activated
                // validate if they must be shown or not
                if (Products.ACTIVATEINDIVIDUAL.get()) {
                    if (!"true".equalsIgnoreCase(getProperty(_parameter, "IncludeIndividual"))) {
                        _queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type,
                                        CIProducts.ProductIndividual.getType().getId(),
                                        CIProducts.ProductBatch.getType().getId());
                    }
                }
                if (Products.FAMILY_ACTIVATE.get()) {
                    final IFilterList filterList = (IFilterList) _parameter.get(ParameterValues.OTHERS);
                    for (final IFilter filter : filterList) {
                        if (filter instanceof final IMapFilter mapFilter && "productFamilyLink".equals(org.efaps.admin.ui.field.Field
                                        .get(filter.getFieldId()).getName())) {
                            if (mapFilter.containsKey("selectedRow")) {
                                if (!mapFilter.containsKey("selecteAll")) {
                                    final String[] oids = (String[]) mapFilter.get("selectedRow");
                                    final List<Instance> familyInsts = new ArrayList<>();
                                    for (final String oid : oids) {
                                        final Instance instance = Instance.get(oid);
                                        if (instance.isValid()) {
                                            familyInsts.add(instance);
                                        }
                                    }
                                    final List<Instance> tmpInsts = new ArrayList<>();
                                    tmpInsts.addAll(familyInsts);
                                    for (final Instance inst : familyInsts) {
                                        tmpInsts.addAll(ProductFamily.getDescendants(_parameter, inst));
                                    }
                                    _queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ProductFamilyLink,
                                                    tmpInsts.toArray());
                                }
                            } else {
                                _queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ID, 0);
                            }
                        }
                    }
                }
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * Product multi print.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return individualInventoryMultiPrint(final Parameter _parameter)
        throws EFapsException
    {
        final MultiPrint multi = new MultiPrint()
        {

            @Override
            protected void add2QueryBldr(final Parameter _parameter,
                                         final QueryBuilder _queryBldr)
                throws EFapsException
            {
                super.add2QueryBldr(_parameter, _queryBldr);
                final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract2IndividualAbstract);
                queryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract,
                                _parameter.getInstance());

                _queryBldr.addWhereAttrInQuery(CIProducts.InventoryAbstract.Product, queryBldr
                                .getAttributeQuery(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract));
            }
        };
        return multi.execute(_parameter);
    }

    /**
     * Gets the sets the filtered families ui value.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the sets the filtered families ui value
     * @throws EFapsException on error
     */
    public Return getSetFilteredFamiliesUIValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        StringBuilder js = new StringBuilder();
        final UIForm uiform = (UIForm) _parameter.get(ParameterValues.CLASS);
        final String sessKey = uiform.getCallingCommand().getUUID() + "-" + UITable.UserCacheKey.FILTER.getValue();
        final String gridXKey = uiform.getCallingCommand().getUUID() + "-" + CacheKey.DBFILTER.getValue();
        String[] oids = null;
        if (Context.getThreadContext().containsSessionAttribute(sessKey)) {
            @SuppressWarnings("unchecked") final Map<String, TableFilter> sessfilter = (Map<String, TableFilter>) Context
                            .getThreadContext().getSessionAttribute(sessKey);
            if (sessfilter.containsKey("productFamilyLink")) {
                final TableFilter tfilter = sessfilter.get("productFamilyLink");
                final IMapFilter filter = (IMapFilter) tfilter.getFilter();
                if (filter != null && filter.containsKey("selectedRow")) {
                    oids = (String[]) filter.get("selectedRow");
                }
            }
        } else if (Context.getThreadContext().containsSessionAttribute(gridXKey)) {
            final IFilterList filterList = (IFilterList) Context.getThreadContext().getSessionAttribute(gridXKey);
            final Optional<IFilter> optional = filterList.stream()
                            .filter(x -> {
                                try {
                                    return org.efaps.admin.ui.field.Field
                                                    .get(x.getFieldId()).getName().equals("productFamilyLink");
                                } catch (final Exception ex) {
                                }
                                return false;
                            })
                            .findFirst();
            if (optional.isPresent()) {
                oids = (String[]) ((IMapFilter) optional.get()).get("selectedRow");
            }
        }

        if (ArrayUtils.isNotEmpty(oids)) {
            final StringBuilder oidJs = new StringBuilder()
                            .append("var selected = [");
            final List<Instance> familyInsts = new ArrayList<>();
            for (final String oid : oids) {
                final Instance instance = Instance.get(oid);
                if (instance.isValid()) {
                    familyInsts.add(instance);
                }
            }
            final List<Instance> tmpInsts = new ArrayList<>();
            tmpInsts.addAll(familyInsts);
            for (final Instance inst : familyInsts) {
                tmpInsts.addAll(ProductFamily.getDescendants(_parameter, inst));
            }
            boolean first = true;
            for (final Instance inst : tmpInsts) {
                if (first) {
                    first = false;
                } else {
                    oidJs.append(",");
                }
                oidJs.append("'").append(inst.getOid()).append("'");
            }
            oidJs.append("];\n");

            if (ElementType.GRID.equals(uiform.getElements().get(0).getType())) {
                final UIFieldGrid grid = (UIFieldGrid) uiform.getElements().get(0).getElement();
                final List<GridRow> rows = grid.getValues();
                final Map<Instance, String> inst2key = new HashMap<>();
                tmpInsts.forEach(inst -> inst2key.put(inst, null));
                eval(inst2key, rows, "");
                js.append("var g = registry.byId('").append(grid.getMarkupId()).append("');\n");

                for (final Entry<Instance, String> entry : inst2key.entrySet()) {
                    js.append("g.select.row.selectById('").append(entry.getValue()).append("');\n");
                }
            } else {
                js.append(oidJs)
                                .append("var nl = query(\"[name='selectedRow']\").forEach(function(node){\n")
                                .append("if (array.indexOf(selected, node.value) > -1) {\n")
                                .append("domAttr.set(node, \"checked\", \"checked\");\n")
                                .append("};\n")
                                .append("});\n");
            }
            js = InterfaceUtils.wrapInDojoRequire(_parameter, js, DojoLibs.QUERY, DojoLibs.DOMATTR, DojoLibs.ARRAY,
                            DojoLibs.REGISTRY);
        }
        if (!ElementType.GRID.equals(uiform.getElements().get(0).getType())) {
            js.append("_container_.onLoadDeferred.then(function() {\n")
                            .append("setTimeout(function(){\n")
                            .append("positionTableColumns(eFapsTable100);\n")
                            .append("}, 500);\n")
                            .append("});\n");
        }
        ret.put(ReturnValues.SNIPLETT, InterfaceUtils.wrappInScriptTag(_parameter, js, true, 1000));
        return ret;
    }

    protected void eval(final Map<Instance, String> _inst2key,
                        final List<GridRow> _rows,
                        final String _key)
    {
        int i = 0;
        for (final GridRow row : _rows) {
            if (_inst2key.containsKey(row.getInstance())) {
                _inst2key.put(row.getInstance(), _key.length() == 0 ? "" + i : _key + "-" + i);
            }
            eval(_inst2key, row.getChildren(), _key.length() == 0 ? "" + i : _key + "-" + i);
            i++;
        }
    }

    /**
     * Individual access check.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return individualAccessCheck(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final PrintQuery print = CachedPrintQuery.get4Request(_parameter.getInstance());
        print.addAttribute(CIProducts.ProductAbstract.Individual);
        print.execute();
        final Object obj = print.getAttribute(CIProducts.ProductAbstract.Individual);

        final Collection<String> indivuals = analyseProperty(_parameter, "Individual").values();
        for (final String indivualStr : indivuals) {
            final ProductIndividual individual = EnumUtils.getEnum(Products.ProductIndividual.class, indivualStr);
            switch (individual) {
                case BATCH:
                    if (ProductIndividual.BATCH.equals(obj)) {
                        ret.put(ReturnValues.TRUE, true);
                        break;
                    }
                    break;
                case INDIVIDUAL:
                    if (ProductIndividual.INDIVIDUAL.equals(obj)) {
                        ret.put(ReturnValues.TRUE, true);
                        break;
                    }
                    break;
                default:
                    if (ProductIndividual.NONE.equals(obj) || obj == null) {
                        ret.put(ReturnValues.TRUE, true);
                        break;
                    }
                    break;
            }
        }
        return ret;
    }

    /**
     * Update fields4 product.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return updateFields4Product(final Parameter _parameter)
        throws EFapsException
    {
        final Return retVal = new Return();
        final List<Map<String, Object>> list = new ArrayList<>();
        final Map<String, Object> map = new HashMap<>();

        final int selected = getSelectedRow(_parameter);
        final String prodOid = _parameter.getParameterValues("product")[selected];
        // validate that a product was selected
        if (prodOid.length() > 0) {
            final PrintQuery print = new PrintQuery(prodOid);
            print.addAttribute(CIProducts.ProductAbstract.Name, CIProducts.ProductAbstract.Description,
                            CIProducts.ProductAbstract.Dimension, CIProducts.ProductAbstract.DefaultUoM);
            print.execute();
            map.put("productAutoComplete", StringEscapeUtils.escapeEcmaScript(print.<String>getAttribute(
                            CIProducts.ProductAbstract.Name)));
            map.put("productDesc", StringEscapeUtils.escapeEcmaScript(print.<String>getAttribute(
                            CIProducts.ProductAbstract.Description)));
            final Long dimId = print.<Long>getAttribute(CIProducts.ProductAbstract.Dimension);
            final Long dUoMId = print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM);
            final long selectedUoM;
            if (dUoMId == null) {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            } else if (Dimension.getUoM(dUoMId).getDimension().equals(Dimension.get(dimId))) {
                selectedUoM = dUoMId;
            } else {
                selectedUoM = Dimension.get(dimId).getBaseUoM().getId();
            }
            map.put("uoM", getUoMFieldStr(selectedUoM, dimId));
            add2updateFields4Product(_parameter, map);
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
        } else {
            map.put("productAutoComplete", "");
            list.add(map);
            retVal.put(ReturnValues.VALUES, list);
            final StringBuilder js = new StringBuilder();
            js.append("document.getElementsByName('productAutoComplete')[").append(selected).append("].focus()");
            map.put("eFapsFieldUpdateJS", js.toString());
        }
        return retVal;
    }

    /**
     * Add to update fields for product.
     *
     * @param _parameter the _parameter
     * @param _map the _map
     * @throws EFapsException the eFaps exception
     */
    protected void add2updateFields4Product(final Parameter _parameter,
                                            final Map<String, Object> _map)
        throws EFapsException
    {
        // to be used by implementations
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
                            .append(StringEscapeUtils.escapeEcmaScript((String) print.getAttribute("Name")))
                            .append("';")
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

    /**
     * Auto complete4 massive products in storage.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return autoComplete4MassiveProductsInStorage(final Parameter _parameter)
        throws EFapsException
    {
        final Instance storage = (Instance) Context.getThreadContext()
                        .getSessionAttribute(Transaction_Base.STORAGEINSTKEY);
        final Instance instance = _parameter.getInstance();
        if (storage != null && storage.isValid() && instance == null) {
            _parameter.put(ParameterValues.INSTANCE, storage);
        } else if (_parameter.getCallInstance() != null && _parameter.getCallInstance().isValid()
                        && _parameter.getCallInstance().getType().isKindOf(CIProducts.StorageAbstract)) {
            _parameter.put(ParameterValues.INSTANCE, _parameter.getCallInstance());
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
        Instance storInst = _parameter.getInstance();
        if (InstanceUtils.isNotValid(storInst)) {
            storInst = _parameter.getCallInstance();
        }
        final List<Map<String, String>> list = new ArrayList<>();
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        if (input.length() > 0) {
            final boolean nameSearch = Character.isDigit(input.charAt(0));
            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            if (nameSearch) {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Name, input + "*").setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Name);
            } else {
                queryBldr.addWhereAttrMatchValue(CIProducts.ProductAbstract.Description, input + "*")
                                .setIgnoreCase(true);
                queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.Description);
            }
            if (Products.ACTIVATEINDIVIDUAL.get()) {
                final QueryBuilder inQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.InventoryIndividual);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storInst);

                final QueryBuilder prodAttrQueryBldr = new QueryBuilder(CIProducts.ProductAbstract);
                prodAttrQueryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type,
                                CIProducts.ProductIndividual.getType().getId(),
                                CIProducts.ProductBatch.getType().getId());
                prodAttrQueryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Individual,
                                ProductIndividual.BATCH, ProductIndividual.INDIVIDUAL);

                final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIProducts.Inventory);
                attrQueryBldr2.addWhereAttrEqValue(CIProducts.Inventory.Storage, storInst);
                attrQueryBldr2.addWhereAttrInQuery(CIProducts.Inventory.Product,
                                prodAttrQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID));

                inQueryBldr.setOr(true);
                inQueryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProducts.Inventory.Product));
                inQueryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                attrQueryBldr2.getAttributeQuery(CIProducts.Inventory.Product));

                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                inQueryBldr.getAttributeQuery(CIProducts.ProductAbstract.ID));

            } else {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIProducts.Inventory);
                attrQueryBldr.addWhereAttrEqValue(CIProducts.Inventory.Storage, storInst);
                queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                                attrQueryBldr.getAttributeQuery(CIProducts.Inventory.Product));
            }
            InterfaceUtils.addMaxResult2QueryBuilder4AutoComplete(_parameter, queryBldr);

            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIProducts.ProductAbstract.ID, CIProducts.ProductAbstract.Name,
                            CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Dimension);
            multi.execute();
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CIProducts.ProductAbstract.Name);
                final String desc = multi.<String>getAttribute(CIProducts.ProductAbstract.Description);
                final String choice = nameSearch ? name + " - " + desc : desc + " - " + name;
                final Map<String, String> map = new HashMap<>();
                map.put("eFapsAutoCompleteKEY", multi.getCurrentInstance().getOid());
                map.put("eFapsAutoCompleteVALUE", name);
                map.put("eFapsAutoCompleteCHOICE", choice);
                map.put("uoM", getUoMFieldStr(multi.<Long>getAttribute(CIProducts.ProductAbstract.Dimension)));
                list.add(map);
            }
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Method is used to make a clone of a product into another product. e.g.
     * from a standard product into a unique product.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance instance to be cloned
     * @param _cloneType type of the cloned product
     * @param _attrMap map of attributes that will be set instead of the value
     *            from the original
     * @param _addClassifications the add classifications
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

        final Set<String> addedAttributes = new HashSet<>();

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
            final Set<Classification> classTypes = new HashSet<>();
            Type curr = _instance.getType();
            while (curr.getParentType() != null) {
                classTypes.addAll(curr.getClassifiedByTypes());
                curr = curr.getParentType();
            }
            classTypes.addAll(curr.getClassifiedByTypes());
            final Set<String> oids = new HashSet<>();
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
                                    if (_attrMap.containsKey(subClassType.getName() + "_" + attr.getName())) {
                                        subInsert.add(attr, _attrMap.get(subClassType.getName()
                                                        + "_" + attr.getName()));
                                    } else {
                                        final Object object = multi.getAttribute(attr);
                                        if (object instanceof Object[]) {
                                            subInsert.add(attr, (Object[]) object);
                                        } else {
                                            subInsert.add(attr, object);
                                        }
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

    /**
     * Creates the relation.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _parent the parent
     * @param _child the child
     * @throws EFapsException on error
     */
    protected void createRelation(final Parameter _parameter,
                                  final Instance _parent,
                                  final Instance _child)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        if (properties.containsKey("CloneConnectType")) {
            final String childAttr = (String) properties.get("CloneConnectChildAttribute");
            final String parentAttr = (String) properties.get("CloneConnectParentAttribute");
            final Type type = Type.get((String) properties.get("CloneConnectType"));

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
                        + StringUtils.join(_attr.getSqlColNames(), "-");
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
        final IUIValue fieldValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
        final TreeMap<String, Long> map = new TreeMap<>();
        long actual = 0;

        // Sales_Configuration
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
            map.put(multi.<String>getAttribute(CIProducts.StorageAbstract.Name),
                            multi.<Long>getAttribute(CIProducts.StorageAbstract.ID));
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
    public Return validate(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<IWarning> warnings = new ArrayList<>();
        final String name = getNameFromUI(_parameter, _parameter.getInstance());
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Name, name);
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()
                        && _parameter.getInstance().getType().isKindOf(CIProducts.ProductAbstract)) {
            queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.ID, _parameter.getInstance());
        }
        if (!queryBldr.getQuery().execute().isEmpty()) {
            warnings.add(new ProductNameInvalidWarning());
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
     * Gets the name from ui.
     *
     * @param _parameter the _parameter
     * @param _instance the _instance
     * @return the name from ui
     * @throws EFapsException on error
     */
    protected String getNameFromUI(final Parameter _parameter,
                                   final Instance _instance)
        throws EFapsException
    {
        String ret = null;
        if (_parameter.getParameterValue(CIFormProducts.Products_ProductForm.name.name) != null) {
            ret = _parameter.getParameterValue(CIFormProducts.Products_ProductForm.name.name);
        } else if (isFamilyActivated(_parameter, _instance)) {
            Instance instance = _instance;
            if (instance == null) {
                final Object obj = _parameter.get(ParameterValues.UIOBJECT);
                if (obj instanceof AbstractCommand) {
                    final Type type = ((AbstractCommand) obj).getTargetCreateType();
                    instance = Instance.get(type, 0);
                }
            }

            final Instance famInst = Instance.get(_parameter.getParameterValue(
                            CIFormProducts.Products_ProductForm.productFamilyLink.name));
            String codePartFam = null;
            if (famInst.isValid()) {
                codePartFam = new ProductFamily().getCode(_parameter, famInst);
            } else if (_parameter.getInstance() != null && _parameter.getInstance().isValid() && _parameter
                            .getInstance().getType().isKindOf(CIProducts.ProductAbstract)) {
                codePartFam = new ProductFamily().getCode(_parameter, _parameter.getInstance());
            }
            if (codePartFam != null) {
                String suffix = _parameter.getParameterValue(CIFormProducts.Products_ProductForm.nameSuffix.name);
                if (suffix == null) {
                    suffix = _parameter.getParameterValue(CIFormProducts.Products_ProductForm.nameSuffix4Edit.name);
                }
                String format = null;
                if (instance.getType().isCIType(CIProducts.ProductMaterial)) {
                    format = Products.MATERIAL_NAMEFRMT.get();
                } else if (instance.getType().isCIType(CIProducts.ProductStandart)) {
                    format = Products.STANDART_NAMEFRMT.get();
                } else if (instance.getType().isCIType(CIProducts.ProductGeneric)) {
                    format = Products.GENERIC_NAMEFRMT.get();
                } else if (instance.getType().isCIType(CIProducts.ProductService)) {
                    format = Products.SERV_NAMEFRMT.get();
                } else if (instance.getType().isCIType(CIProducts.ProductInfinite)) {
                    format = Products.INFINITE_NAMEFRMT.get();
                }
                if (format == null) {
                    ret = codePartFam + "." + suffix;
                } else {
                    ret = String.format(format, codePartFam, suffix);
                }
            }

            if (instance.getType().isCIType(CIProducts.ProductMaterial)) {
                ret = Products.MATERIAL_FAMPRE.get() == null ? ret : Products.MATERIAL_FAMPRE.get() + ret;
            } else if (instance.getType().isCIType(CIProducts.ProductStandart)) {
                ret = Products.STANDART_FAMPRE.get() == null ? ret : Products.STANDART_FAMPRE.get() + ret;
            } else if (instance.getType().isCIType(CIProducts.ProductGeneric)) {
                ret = Products.GENERICFAMPRE.get() == null ? ret : Products.GENERICFAMPRE.get() + ret;
            } else if (instance.getType().isCIType(CIProducts.ProductService)) {
                ret = Products.SERV_FAMPRE.get() == null ? ret : Products.SERV_FAMPRE.get() + ret;
            } else if (instance.getType().isCIType(CIProducts.ProductInfinite)) {
                ret = Products.INFINITEFAMPRE.get() == null ? ret : Products.INFINITEFAMPRE.get() + ret;
            }
        }
        return ret;
    }

    /**
     * Gets the costing for a product.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the costing4 product
     * @throws EFapsException on error
     */
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

    /**
     * Check4 individual.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return check4Individual(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getInstance();
        if (inst != null && inst.isValid() && inst.getType().isKindOf(CIProducts.ProductAbstract)) {
            final PrintQuery print = CachedPrintQuery.get4Request(inst);
            print.addAttribute(CIProducts.ProductAbstract.Individual);
            print.execute();
            final Object indi = print.getAttribute(CIProducts.ProductAbstract.Individual);
            if (indi != null && indi instanceof ProductIndividual
                            && (ProductIndividual.INDIVIDUAL.equals(indi) || ProductIndividual.BATCH.equals(indi))) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Gets the field format field value ui.
     *
     * @param _parameter the _parameter
     * @return the field format field value ui
     * @throws EFapsException on error
     */
    public Return getFieldFormatFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        String name = null;
        final PrintQuery print = new PrintQuery(_parameter.getInstance());
        print.addAttribute(CIProducts.ProductAbstract.Name);
        if (print.execute()) {
            final String nameTmp = print.getAttribute(CIProducts.ProductAbstract.Name);
            final String[] nameAr = nameTmp.split("\\.");
            name = nameAr[nameAr.length - 1];
        }
        final Return ret = new Return();
        ret.put(ReturnValues.VALUES, name);
        return ret;
    }

    /**
     * Gets the suffix4 family.
     *
     * @param _parameter the _parameter
     * @param _famInst the _fam inst
     * @return the suffix4 family
     * @throws EFapsException on error
     */
    public String getSuffix4Family(final Parameter _parameter,
                                   final Instance _famInst)
        throws EFapsException
    {
        Integer length = Products.FAMILY_SUFFIXLENGTH.get();
        if (length == null || length < 1) {
            length = 3;
        }
        Integer val = 1;
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        // do not include the individual products, because they have their own
        // naming
        queryBldr.addWhereAttrNotEqValue(CIProducts.ProductAbstract.Type, CIProducts.ProductBatch.getType().getId(),
                        CIProducts.ProductIndividual.getType().getId());
        queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.ProductFamilyLink, _famInst);
        queryBldr.addOrderByAttributeDesc(CIProducts.ProductAbstract.Name);
        queryBldr.setLimit(1);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.ProductAbstract.Name);
        multi.execute();
        while (multi.next()) {
            final String name = multi.getAttribute(CIProducts.ProductAbstract.Name);
            final String[] nameAr = name.split("\\.");
            final String numStr = nameAr[nameAr.length - 1];
            if (StringUtils.isNumeric(numStr)) {
                val = Integer.parseInt(numStr) + 1;
            }
        }
        final NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(length);
        nf.setMaximumIntegerDigits(length);
        nf.setGroupingUsed(false);
        return nf.format(val);
    }

    /**
     * Gets the instances for the replacement products of a generic.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _genericInst the instance of the generic product
     * @return the replacements 4 generic
     * @throws EFapsException on error
     */
    public List<Instance> getReplacements4Generic(final Parameter _parameter,
                                                  final Instance _genericInst)
        throws EFapsException
    {
        final List<Instance> ret = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductGeneric2Product);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductGeneric2Product.FromLink, _genericInst);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selInst = SelectBuilder.get().linkto(CIProducts.ProductGeneric2Product.ToLink).instance();
        multi.addSelect(selInst);
        multi.execute();
        while (multi.next()) {
            final Instance prodInst = multi.getSelect(selInst);
            if (prodInst != null && prodInst.isValid()) {
                ret.add(prodInst);
            }
        }
        return ret;
    }

    /**
     * Gets the instance for the generic product of a replacement.
     *
     * @param _parameter the _parameter
     * @param _individualProdInst the individual prod inst
     * @return the generic4 replacment
     * @throws EFapsException on error
     */
    public Instance getProduct4Individual(final Parameter _parameter,
                                          final Instance _individualProdInst)
        throws EFapsException
    {
        Instance ret = null;
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.StoreableProductAbstract2IndividualAbstract);
        queryBldr.addWhereAttrEqValue(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract,
                        _individualProdInst);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selInst = SelectBuilder.get().linkto(
                        CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract).instance();
        multi.addSelect(selInst);
        multi.execute();
        while (multi.next()) {
            final Instance prodInst = multi.getSelect(selInst);
            if (prodInst != null) {
                ret = prodInst;
            }
        }
        return ret == null ? Instance.get("") : ret;
    }

    /**
     * Gets the instance for the generic product of a replacement.
     *
     * @param _parameter the _parameter
     * @param _replInst the _generic inst
     * @return the generic4 replacment
     * @throws EFapsException on error
     */
    public Instance getGeneric4Replacment(final Parameter _parameter,
                                          final Instance _replInst)
        throws EFapsException
    {
        Instance ret = null;
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductGeneric2Product);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductGeneric2Product.ToLink, _replInst);

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selInst = SelectBuilder.get().linkto(CIProducts.ProductGeneric2Product.FromLink).instance();
        multi.addSelect(selInst);
        multi.execute();
        while (multi.next()) {
            final Instance prodInst = multi.getSelect(selInst);
            if (prodInst != null) {
                ret = prodInst;
            }
        }
        return ret == null ? Instance.get("") : ret;
    }

    /**
     * Assign generic.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return assignGeneric(final Parameter _parameter)
        throws EFapsException
    {
        final Instance genericInst = Instance.get(_parameter.getParameterValue(
                        CIFormProducts.Products_ProductGenericAssignForm.generic.name));
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductGeneric2Product);
        queryBldr.addWhereAttrEqValue(CIProducts.ProductGeneric2Product.ToLink, _parameter.getInstance());
        for (final Instance inst : queryBldr.getQuery().execute()) {
            new Delete(inst).execute();
        }

        if (genericInst != null && genericInst.isValid()) {
            final Insert insert = new Insert(CIProducts.ProductGeneric2Product);
            insert.add(CIProducts.ProductGeneric2Product.ToLink, _parameter.getInstance());
            insert.add(CIProducts.ProductGeneric2Product.FromLink, genericInst);
            insert.execute();
        }
        return new Return();
    }

    /**
     * Sets the description.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return setDescription(final Parameter _parameter)
        throws EFapsException
    {
        final AbstractCommand cmd = (AbstractCommand) _parameter.get(ParameterValues.CALL_CMD);
        final Form form = cmd.getTargetForm();
        Type type = null;
        if (_parameter.getInstance() != null && _parameter.getInstance().isValid()) {
            type = _parameter.getInstance().getType();
        } else {
            type = cmd.getTargetCreateType();
        }
        final Properties descriptions;
        if (type.isCIType(CIProducts.ProductStandart)) {
            descriptions = Products.STANDART_DESCR.get();
        } else if (type.isCIType(CIProducts.ProductMaterial)) {
            descriptions = Products.MATERIAL_DESCR.get();
        } else if (type.isCIType(CIProducts.ProductService)) {
            descriptions = Products.SERV_DESCR.get();
        } else if (type.isCIType(CIProducts.ProductInfinite)) {
            descriptions = Products.INFINITEDESCR.get();
        } else {
            descriptions = new Properties();
        }

        String text = descriptions.getProperty("Default", "Default");

        // main fields
        final Map<String, String> valueMap = new HashMap<>();
        for (final org.efaps.admin.ui.field.Field field : form.getFields()) {
            valueMap.put(field.getName(), getSubstitutionValue4Description(_parameter, type, field));
        }

        final Set<Classification> clazzes = type.getClassifiedByTypes();
        for (final Classification clazz : clazzes) {
            if (descriptions.containsKey(clazz.getName())) {
                text = descriptions.getProperty(clazz.getName());
                final Form clazzForm = clazz.getTypeForm();
                for (final org.efaps.admin.ui.field.Field field : clazzForm.getFields()) {
                    valueMap.put(field.getName(), getSubstitutionValue4Description(_parameter, clazz, field));
                }
            }
        }

        final String fieldName = getProperty(_parameter, "FieldName", "description");
        final Return ret = new Return();
        final StringBuilder js = new StringBuilder();
        js.append(getSetFieldValue(0, fieldName, StringSubstitutor.replace(text, valueMap).replace("  ", " ")));
        ret.put(ReturnValues.SNIPLETT, js.toString());
        return ret;
    }

    /**
     * Gets the substitution value for description.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _type the type
     * @param _field the field
     * @return the substitution value4 description
     * @throws EFapsException on error
     */
    protected String getSubstitutionValue4Description(final Parameter _parameter,
                                                      final Type _type,
                                                      final org.efaps.admin.ui.field.Field _field)
        throws EFapsException
    {
        String ret = _parameter.getParameterValue(_field.getName());
        if (ret != null && !StringUtils.isEmpty(_field.getAttribute())) {
            final Attribute attr = _type.getAttribute(_field.getAttribute());
            if (attr.getAttributeType().getUIProvider() instanceof LinkWithRangesUI) {
                @SuppressWarnings("unchecked") final List<IOption> options = (List<IOption>) attr.getAttributeType()
                                .getUIProvider().getValue(
                                                UIValue.get(_field, attr, ret));
                for (final IOption option : options) {
                    if (String.valueOf(option.getValue()).equals(ret)) {
                        ret = option.getLabel();
                    }
                }
            }
        }
        return ret;
    }

    @Override
    public ITableProvider init(final AbstractUserInterfaceObject cmd,
                               final List<org.efaps.admin.ui.field.Field> fields,
                               final Map<String, String> properties,
                               final TargetMode targetMode,
                               final String oid)
        throws EFapsException
    {
        LOG.info("evaluating: {}", cmd.getName());
        if (cmd.getName().equals("inventoryIndividualTable")) {
            tableProvider = new StandardTableProvider()
            {

                @Override
                protected Query evalQuery(List<Type> types)
                    throws EFapsException
                {
                    final var query = super.evalQuery(types);

                    final var subQuery = EQL.builder()
                                    .nestedQuery(CIProducts.StoreableProductAbstract2IndividualAbstract)
                                    .where()
                                    .attribute(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract)
                                    .eq(Instance.get(oid)).up()
                                    .selectable(Selectables.attribute(
                                                    CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract));

                    query.where().attribute(CIProducts.InventoryAbstract.Product).in(subQuery);
                    return query;
                }
            };
        } else {
            tableProvider = new StandardTableProvider();
        }
        return tableProvider.init(cmd, fields, properties, targetMode, oid);
    }

    @Override
    public Collection<Map<String, ?>> getValues()
        throws EFapsException
    {
        return tableProvider.getValues();
    }

    /**
     * Warning for invalid name.
     */
    public static class ProductNameInvalidWarning
        extends AbstractWarning
    {

        /**
         * Constructor.
         */
        public ProductNameInvalidWarning()
        {
            setError(true);
        }
    }
}
