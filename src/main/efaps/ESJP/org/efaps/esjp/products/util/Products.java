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

package org.efaps.esjp.products.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSysConfLink;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.IntegerSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.PropertiesSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.StringSysConfAttribute;
import org.efaps.esjp.admin.common.systemconfiguration.SysConfLink;
import org.efaps.util.cache.CacheReloadException;

/**
 * TODO comment!
 *
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
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ActivateIndividual")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEINFINITE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Infinite.Activate")
                    .description("Activate the Infiniteproduct managements.")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEPRICEMASSUP = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "ActivatePriceMassUpdate")
                    .description(" Activate the menu for updating Product Prices on mass.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEPRICEGRP = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "PriceGroup.Activate")
                    .description("Activate the price group management.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEFAMILY = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Family.Activate")
                    .description(" Activate the individual management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute FAMILYSUFFIXLENGTH = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Family.SuffixLength")
                    .defaultValue(3)
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEVARIANT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Variant.Activate")
                    .description(" Activate the variant management menu in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute VARIANTCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Variant.Configuration")
                    .description(" Configuration for the Variant mechanism.")
                    .concatenate(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute VARIANTACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Variant.ActivateFamilies")
                    .description("Activate the family management for generics.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute VARIANTFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Variant.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute VARIANTACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Variant.ActivateClassification")
                    .description("Activate the classifcation for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.Activate")
                    .description(" Activate the generic product management in general.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute GENERICACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.ActivateFamilies")
                    .description("Activate the family management for generics.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute GENERICFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Generic.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute INFINITEACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Infinite.ActivateFamilies")
                    .description("Activate the family management for Infinite products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute INFINITEFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Infinite.FamiliesPrefix")
                    .description("Activate the family management for Infinite products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute INFINITEDESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Infinite.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATEMATERIAL = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.Activate")
                    .description("Activate the generic product management for materials.")
                    .defaultValue(true);

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALISGENERIC = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.IsGeneric")
                    .description("Activate the generic product management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.ActivateFamilies")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.ActivateClassification")
                    .description("Activate the classifcation for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute MATERIALACTIND = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.ActivateIndividual")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute MATERIALFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute MATERIALDESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Material.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDARTACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Standart.ActivateFamilies")
                    .description("Activate the family management for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute STANDARTACTCLASS = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Standart.ActivateClassification")
                    .description("Activate the classifcation for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STANDARTDESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Standart.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute STANDARTFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Standart.FamiliesPrefix")
                    .description("Activate the family management for standart products.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute STANDARTIMG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Standart.Image")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");


    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute SERVACTFAM = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Service.ActivateFamilies")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final StringSysConfAttribute SERVFAMPRE = new StringSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Service.FamiliesPrefix")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute SERVDESCR = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Service.Descriptions")
                    .description("Substitutor values: Default=${Name}, Products_ProductStandartClass=text ${name} .");


    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute TREEVIEWACT = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "TreeView.Activate")
                    .description("Activate the TreeView.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final IntegerSysConfAttribute REPINVENTORYCLASSLEVEL = new IntegerSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.InventoryReport.ClassificationLevel")
                    .description("Level of Classification to present.");

    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPCOSTCONFIG = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.CostReport")
                    .description("Configuration for CostReport.");


    /** See description. */
    @EFapsSysConfAttribute
    public static final PropertiesSysConfAttribute REPLASTMOVE = new PropertiesSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "report.LastMovementReport")
                    .description("Configuration for LastMovementReport.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTDIMENSION = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultDimension")
                    .description("Activate the family management for materials.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTWAREHOUSE = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultWareHouse")
                    .description("Link to a default warehouse instance.");

    /** See description. */
    @EFapsSysConfLink
    public static final SysConfLink DEFAULTSTORAGEGRP4BOM = new SysConfLink()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "DefaultStorageGroup4BOMCalculator")
                    .description("Link to a default StorageGroup instance used by the BOMCalculator.");

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
     * @return the SystemConfigruation for Sales
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        return SystemConfiguration.get(SYSCONFUUID);
    }
}
