## TC 1.2: Startup with invalid parameter

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module fails to authenticate the http call by verifying the digital signature.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The http call to the vaccination module misses one or many required parameter.

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
