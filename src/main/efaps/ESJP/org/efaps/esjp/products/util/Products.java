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
package org.efaps.esjp.products.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSysConfLink;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.EnumSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.ListSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.esjp.products.ProductFamily_Base.NameDefinition;
import org.efaps.util.cache.CacheReloadException;

/**
 * @author The eFaps Team
 */
@EFapsUUID("96b4a9bc-bfcf-41fa-9c03-4dbc6279cf63")
@EFapsApplication("eFapsApp-Products")
@EFapsSystemConfiguration("e53cd705-e463-47dc-a400-4ace4ed72071")
public final class Products
{
    /** The base. */
    public static final String BASE = "org.efaps.products.";

    /** Products-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("e53cd705-e463-47dc-a400-4ace4ed72071");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEINDIVIDUAL = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "ActivateIndividual")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEINFINITE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Infinite.Activate")
                    .description("Activate the Infiniteproduct managements.")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEPRICEMASSUP = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "ActivatePriceMassUpdate")
                    .description(" Activate the menu for updating Product Prices on mass.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEPRICEGRP = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "PriceGroup.Activate")
                    .description("Activate the price group management.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute BATCH_ACTARCHIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Batch.ActivateArchives")
                    .defaultValue(true)
                    .description("Activate the possibility to relate arcives to a batch product. "
                                    + "Only works if eFapsApp-Archives is installed also.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute COSTACTIVATEALT = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Cost.ActivateAlternative")
                    .description(" Activate the possibility to register alterntative costs.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FAMILY_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.Activate")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FAMILY_ACTIVATE_UNSPSC = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.ActivateUNSPSC")
                    .description("Activate the use of UNSPSC.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute FAMILY_SUFFIXLENGTH = new IntegerSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.SuffixLength")
                    .defaultValue(3)
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute FAMILY_NAMESEP = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.NameSeparator")
                    .defaultValue(" - ")
                    .description("Seperator String used to seperate the diffenert parts of a complete family name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute FAMILY_NAMEINCLLINE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.NameIncludeLine")
                    .defaultValue(true)
                    .description("Include the line in the name.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final EnumSysConfAttribute<NameDefinition> FAMILY_NAMEDEF = new EnumSysConfAttribute<NameDefinition>()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Family.NameDefinition")
                    .clazz(NameDefinition.class)
                    .defaultValue(NameDefinition.ALL)
                    .description("Name Definition");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEVARIANT = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Variant.Activate")
                    .description(" Activate the variant management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute VARIANTCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Variant.Configuration")
                    .description(" Configuration for the Variant mechanism.")
                    .concatenate(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute VARIANTACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Variant.ActivateFamilies")
                    .description("Activate the family management for generics.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute VARIANTFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Variant.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute VARIANTACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Variant.ActivateClassification")
                    .description("Activate the classifcation for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Generic.Activate")
                    .description(" Activate the generic product management in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GENERICACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Generic.ActivateFamilies")
                    .description("Activate the family management for generics.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute GENERICFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Generic.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute GENERIC_NAMEFRMT = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Generic.NameFormat")
                    .description("StringFormat to be used to create "
                                    + "the name from FamilyCode (first Parameter) and Name (second Parameter)")
                    .defaultValue("%s.%s");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INDIVIDUAL_ACTARCHIVE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Individual.ActivateArchives")
                    .defaultValue(true)
                    .description("Activate the possibility to relate arcives to a indiviual product. "
                                    + "Only works if eFapsApp-Archives is installed also.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INFINITEACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Infinite.ActivateFamilies")
                    .description("Activate the family management for Infinite products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INFINITEFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Infinite.FamiliesPrefix")
                    .description("Activate the family management for Infinite products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INFINITE_NAMEFRMT = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Infinite.NameFormat")
                    .description("StringFormat to be used to create "
                                    + "the name from FamilyCode (first Parameter) and Name (second Parameter)")
                    .defaultValue("%s.%s");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INFINITEDESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Infinite.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATE_MATERIAL = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.Activate")
                    .description("Activate the generic product management for materials.")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIAL_ISGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.IsGeneric")
                    .description("Activate the generic product management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIAL_ACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.ActivateFamilies")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIAL_ACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.ActivateClassification")
                    .description("Activate the classifcation for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIAL_ACTIND = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.ActivateIndividual")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute MATERIAL_FAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute MATERIAL_NAMEFRMT = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.NameFormat")
                    .description("StringFormat to be used to create "
                                    + "the name from FamilyCode (first Parameter) and Name (second Parameter)")
                    .defaultValue("%s.%s");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute MATERIAL_DESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Material.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateFamilies")
                    .description("Activate the family management for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTNOTE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateNote")
                    .description("Activate the note field for standart products.");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTBARCODES = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateBarcodes")
                    .description("Activate the Barcodes field set for standart products.");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTCONFBOM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateConfigurationBOM")
                    .description("Activate the ConfigurationBOM for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateClassification")
                    .description("Activate the classifcation for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute STANDART_CLASS = new ListSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.RootClassifications")
                    .addDefaultValue("Products_ProductStandartClass")
                    .description("List of root classifications for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDART_ACTIND = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.ActivateIndividual")
                    .description("Activate the individual for standart products. "
                                    + "Also needs the general mechanism activated");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STANDART_CONV = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.Conversion")
                    .addDefaultValue("Dimension01", "bc921c98-9e50-4614-a9c4-83a22fca3105")
                    .description("Activate the conversion management for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STANDART_DESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute STANDART_FAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.FamiliesPrefix")
                    .description("Activate the family management for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute STANDART_NAMEFRMT = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.NameFormat")
                    .description("StringFormat to be used to create "
                                    + "the name from FamilyCode (first Parameter) and Name (second Parameter)")
                    .defaultValue("%s.%s");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STANDART_IMG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Standart.Image")
                    .addDefaultValue("Image4Doc.Create", "false")
                    .addDefaultValue("Image4Doc.Width", "250")
                    .addDefaultValue("Image4Doc.Height", "250")
                    .addDefaultValue("Image4Doc.Enlarge", "false")
                    .addDefaultValue("Thumbnail.Create", "true")
                    .addDefaultValue("Thumbnail.Width", "150")
                    .addDefaultValue("Thumbnail.Height", "150")
                    .addDefaultValue("Thumbnail.Enlarge", "false")
                    .description("Configuration for Image.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.Activate")
                    .description("Activate the SalesPartList.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.ActivateFamilies")
                    .description("Activate the family management for SalesPartList.");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTBARCODES = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.ActivateBarcodes")
                    .description("Activate the Barcodes field set for SalesPartList.");

    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTCONFBOM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.ActivateConfigurationBOM")
                    .description("Activate the ConfigurationBOM for SalesPartList.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTNOTE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.ActivateNote")
                    .description("Activate the note field for SalesPartList.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SALESPARTLIST_ACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.ActivateClassification")
                    .description("Activate the classifcation for SalesPartList.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final ListSysConfAttribute SALESPARTLIST_CLASS = new ListSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.RootClassifications")
                    .description("List of root classifications for SalesPartList.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SALESPARTLIST_IMG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "SalesPartList.Image")
                    .addDefaultValue("Image4Doc.Create", "false")
                    .addDefaultValue("Image4Doc.Width", "250")
                    .addDefaultValue("Image4Doc.Height", "250")
                    .addDefaultValue("Image4Doc.Enlarge", "false")
                    .addDefaultValue("Thumbnail.Create", "true")
                    .addDefaultValue("Thumbnail.Width", "150")
                    .addDefaultValue("Thumbnail.Height", "150")
                    .addDefaultValue("Thumbnail.Enlarge", "false")
                    .description("Configuration for Image.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERV_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.Activate")
                    .description("Activate service products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERV_ACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.ActivateFamilies")
                    .description("Activate the family management for service.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERV_ACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.ActivateClassification")
                    .description("Activate the classifcation for service products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERV_FAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERV_NAMEFRMT = new StringSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.NameFormat")
                    .description("StringFormat to be used to create "
                                    + "the name from FamilyCode (first Parameter) and Name (second Parameter)")
                    .defaultValue("%s.%s");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERV_DESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "Service.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute TEXTPOS_ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "TextPosition.Activate")
                    .description("Activate the Textposition.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute TREEVIEWACT = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "TreeView.Activate")
                    .description("Activate the TreeView.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPINVENTORY_CLASSACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .defaultValue(true)
                    .key(Products.BASE + "report.InventoryReport.Classification.Activate")
                    .description("Activate Classification for the InventoryReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute REPINVENTORY_CLASSLEVEL = new IntegerSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.InventoryReport.ClassificationLevel")
                    .description("Level of Classification to present.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPINVENTORY_FAMILYACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.InventoryReport.Family.Activate")
                    .description("Activate Classification for the InventoryReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPCOSTCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.CostReport")
                    .description("Configuration for CostReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPLASTMOVE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.LastMovementReport")
                    .description("Configuration for LastMovementReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPPRICELIST = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.PriceListReport")
                    .addDefaultValue("ShowClassification", "true")
                    .addDefaultValue("ShowFamily", "false")
                    .addDefaultValue("ActiveProductsOnly", "true")
                    .addDefaultValue("Type", "Products_ProductPricelistRetail")
                    .description("Configuration for PriceListReport.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute REPPRICELIST_ACTBARCODE = new BooleanSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "report.PriceListReport.ActivateBarcodes")
                    .description("Activate Barcodes for PriceListReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTDIMENSION = new SysConfLink()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "DefaultDimension")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "DefaultWareHouse")
                    .description("Link to a default warehouse instance.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTSTORAGEGRP4BOM = new SysConfLink()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "DefaultStorageGroup4BOMCalculator")
                    .description("Link to a default StorageGroup instance used by the BOMCalculator.");

    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute AUTOCOMPLETE = new PropertiesSysConfAttribute()
                    .sysConfUUID(Products.SYSCONFUUID)
                    .key(Products.BASE + "AutocompleteConfig")
                    .addDefaultValue("maxResult", "200")
                    .addDefaultValue("searchBarcodes", "false")
                    .description("""
                        Configuration for Product Autocomplete.
                        maxResult (Integer): maximum result to be shown
                        searchBarcodes (Boolean): activate/deactivate search in barcode""");

    /**
     * Singelton.
     */
    private Products()
    {
    }

    /**
     * The Enum ProductIndividual.
     *
     */
    public enum ProductIndividual
        implements IEnum
    {

        /** The none. */
        NONE,

        /** The individual. */
        INDIVIDUAL,

        /** The batch. */
        BATCH;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    /**
     * The Enum ProductIndividual.
     *
     */
    public enum CostingState
        implements IEnum
    {

        /** The none. */
        ACTIVE,

        /** The individual. */
        FIXED,

        /** The batch. */
        INACTIVE;

        @Override
        public int getInt()
        {
            return ordinal();
        }
    }

    public enum BOMGroupConfig
      implements IBitEnum
    {

        OPTIONAL,
        ONLYONE,
        CHARGEABLE;

        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }

    public enum ConfigurationBOMFlag
        implements IBitEnum
      {

          DEFAULT;

          @Override
          public int getInt()
          {
              return BitEnumType.getInt4Index(ordinal());
          }

          @Override
          public int getBitIndex()
          {
              return ordinal();
          }
  }

    /**
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(Products.SYSCONFUUID);
    }
}
