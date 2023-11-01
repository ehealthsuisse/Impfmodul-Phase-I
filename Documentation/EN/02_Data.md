## Classification of Data

### Master Data

The vaccination module uses the following master data which shall be deployed with the installation package:
- Configuration data, especially the configuration settings for communicating with the community.
- Metadata as value sets for the vaccination profiles.
- I18n files for the translation of the UI elements.  

### Working Data

When run the vaccination module loads vaccination data from the patients EPR and stores them in the computers memory for the session duration. When finishing the session the data in memory are deleted. The vaccination module does not persistently store privacy data outside of the EPR.

### Privacy Data

The vaccination module does not store privacy data. The vaccination module gets privacy data from the portal and uses them for the duration of the session.

The vaccination module retrieves the following privacy data form the portal at startup:
- The patients local ID as used by the portal.
- The name, title and GLN (if available) of the authenticated user.
- The EPR role of the authenticated user.

In addition the following data are retrieved, if the authenticated user acts an assistant:
- The GLN of the health professionals the user assists.
- The Name of the health professionals the user assists.

The above mentioned data are used by the vaccination module for functioning, especially to retrieve an X-User Assertion from the community.
