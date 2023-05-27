## TC 1.3: Startup with invalid parameter

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature. A required parameter is missing or malformatted.  


### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The HMAC signature of the http signature is invalid or the HMAC shared secret is not known by the vaccination module

### Test run:

The test shall be run as follows:

Iteration:
- Start the vaccination module from the EPR portal as assistant.
- Verify that a error message is displayed.

Iteration:
- Start the vaccination module from the EPR portal as health professional.
- Verify that a error message is displayed.

Iteration:
- Start the vaccination module from the EPR portal as patient.
- Verify that a error message is displayed.

Iteration:
- Start the vaccination module from the EPR portal as representative.
- Verify that a error message is displayed.

### Expected result:

The expected result is:
- The vaccination module starts in the same browser window or in a new browser tab.
- The vaccination module displays an error message.
