## TC 1.1: Startup

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.
- A set of vaccination documents for the patient is present in the EPR test system.


### Test run:

The test shall be run as follows:

Iteration:
- Start the vaccination module from the EPR portal as assistant.
- Verify that the vaccination record is displayed in the vaccination module.

Iteration:
- Start the vaccination module from the EPR portal as health professional.
- Verify that the vaccination record is displayed in the vaccination module.

Iteration:
- Start the vaccination module from the EPR portal as patient.
- Verify that the vaccination record is displayed in the vaccination module.

Iteration:
- Start the vaccination module from the EPR portal as representative.
- Verify that the vaccination record is displayed in the vaccination module.


### Expected result:

The expected result is:
- The vaccination module starts in the same browser window or in a new browser tab.
- The vaccination module authenticastes the user at the Identity Provider.
- The vaccination module performs the queries to retriebve the vaccination documents from the EPR (PDQ, Get X-User Assertion, Registry Stored Query, Retrieve Document Set).
- The vaccination module displays the vaccination data retrieved from the patients EPR.
