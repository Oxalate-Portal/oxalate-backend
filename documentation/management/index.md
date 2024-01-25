# Oxalate Portal backend service management

If and when you plan to have the service running in production, the first thing you need to decide is is what platform will you be running it on.

There are several options available:

* Run it in a container provided by a cloud provider (AWS, Google, Azure, etc.)
* Run it in a virtual machine provided by a VM provider (Hetzer, Digital Ocean, etc. in addition to the cloud providers)
  * In a container
  * Natively (not recommended)
* Run it on a bare metal server
  * In a container
  * Natively (not recommended)

Once the platform decision is made, then you should consider the following points:

1. Database backup
2. Log monitoring and analysis
3. Service monitoring
4. Service availability
5. Service security

These are dependant of the platform you choose, as depending of the platform choice, some of these are provided by the platform itself.

## Common tasks

### Database maintenance

Besides database backup, you should also maintain the database itself by making sure you're not running an outdated version of the database software. Practise
doing backup restores since this is the best way to update the software besides the fact that you also will confirm that your chosen backup solution is in fact
working.

### Backend service maintenance

There will periodically be new versions of the backend service available. These will be published as docker images in the GitHub container registry. You should
periodically check if there are new versions available and update the service accordingly. Make sure to read the release notes of the new versions to see if
there are any breaking changes.

### Platform maintenance

Follow the best practices of the platform you're using. For example, if you're running the service in AWS, then be on alert for any security updates and apply
them in a timely fashion. Similarly, if you're running the service in a VM, then make sure that you're running the latest version of the operating system and
keep it patched.
