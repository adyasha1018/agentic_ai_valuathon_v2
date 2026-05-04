package com.dnb.kyc.agents.enrichment;

/**
 * Enumeration of approved data sources for KYC enrichment.
 * 
 * GDPR & EU AML Compliance:
 * Only government registries and official public-sector sources
 * are permitted for enrichment operations.
 */
public enum SourceType {
    
    /**
     * Official government registry (e.g., tax authorities, civil registries)
     */
    GOVERNMENT_REGISTRY("Government Registry", true),
    
    /**
     * Official company/business register (e.g., Netherlands KvK, UK Companies House)
     */
    OFFICIAL_COMPANY_REGISTER("Official Company Register", true),
    
    /**
     * Public Ultimate Beneficial Owner register (EU AML Directive requirement)
     */
    PUBLIC_UBO_REGISTER("Public UBO Register", true),
    
    /**
     * Official address/postal database (e.g., PostNL, government address registry)
     */
    OFFICIAL_ADDRESS_DATABASE("Official Address Database", true),
    
    /**
     * EU-wide business registry interconnection system
     */
    BRIS_REGISTRY("BRIS (Business Registers Interconnection System)", true),
    
    /**
     * Demographic model estimation - SOFT enrichment only
     */
    DEMOGRAPHIC_MODEL("Demographic Model", false),
    
    /**
     * Age-based statistical estimation - SOFT enrichment only
     */
    AGE_BASED_ESTIMATION("Age-Based Estimation", false),
    
    /**
     * Country risk data from official sources
     */
    COUNTRY_REGISTRY("Country Registry", true);

    private final String displayName;
    private final boolean hardSource;

    SourceType(String displayName, boolean hardSource) {
        this.displayName = displayName;
        this.hardSource = hardSource;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this source provides hard (authoritative) data.
     * Hard sources are official registries with legal authority.
     * Soft sources are estimations or statistical models.
     */
    public boolean isHardSource() {
        return hardSource;
    }
}
