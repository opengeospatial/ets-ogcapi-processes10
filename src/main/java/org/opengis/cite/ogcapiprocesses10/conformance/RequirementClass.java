package org.opengis.cite.ogcapiprocesses10.conformance;

/**
 *
 *
 * Encapsulates all known requirement classes.
 *
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public enum RequirementClass {

	CORE("http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core");

	private final String conformanceClass;

	private final String mediaTypeFeaturesAndCollections;

	private final String mediaTypeOtherResources;

	RequirementClass(String conformanceClass) {
		this(conformanceClass, null, null);
	}

	RequirementClass(String conformanceClass, String mediaTypeFeaturesAndCollections, String mediaTypeOtherResources) {
		this.conformanceClass = conformanceClass;
		this.mediaTypeFeaturesAndCollections = mediaTypeFeaturesAndCollections;
		this.mediaTypeOtherResources = mediaTypeOtherResources;
	}

	/**
	 * <p>
	 * hasMediaTypeForFeaturesAndCollections.
	 * </p>
	 * @return <code>true</code> if the RequirementClass has a media type for features and
	 * collections, <code>true</code> otherwise
	 */
	public boolean hasMediaTypeForFeaturesAndCollections() {
		return mediaTypeFeaturesAndCollections != null;
	}

	/**
	 * <p>
	 * Getter for the field <code>mediaTypeFeaturesAndCollections</code>.
	 * </p>
	 * @return media type for features and collections, <code>null</code> if not available
	 */
	public String getMediaTypeFeaturesAndCollections() {
		return mediaTypeFeaturesAndCollections;
	}

	/**
	 * <p>
	 * hasMediaTypeForOtherResources.
	 * </p>
	 * @return <code>true</code> if the RequirementClass has a media type for other
	 * resources, <code>true</code> otherwise
	 */
	public boolean hasMediaTypeForOtherResources() {
		return mediaTypeOtherResources != null;
	}

	/**
	 * <p>
	 * Getter for the field <code>mediaTypeOtherResources</code>.
	 * </p>
	 * @return media type of other resources, <code>null</code> if not available
	 */
	public String getMediaTypeOtherResources() {
		return mediaTypeOtherResources;
	}

	/**
	 * <p>
	 * byConformanceClass.
	 * </p>
	 * @param conformanceClass the conformance class of the RequirementClass to return.
	 * @return the RequirementClass with the passed conformance class, <code>null</code>
	 * if RequirementClass exists
	 */
	public static RequirementClass byConformanceClass(String conformanceClass) {
		for (RequirementClass requirementClass : values()) {
			if (requirementClass.conformanceClass.equals(conformanceClass))
				return requirementClass;
		}
		return null;
	}

}
