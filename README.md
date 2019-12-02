# BearMetal
A Java Reslet API for deploying operating systems to networked devices using iSCSI.

## Disclaimer
This project makes use of proprietary code which was deemed unfit to be uploaded here. 
Namely, a few calls are made to CLIProcess and YMLConfiguration, which are in-house packages. As such, this code will not run as-is, nor is it intended to. 

**If you wish to run this code, you will have to find suitable alternatives for running shell commands and parsing YML.**

## Description
In a typical scenario, malware analysis is performed in a Virtual Machine (VM). This is ideal because it (hopefully) provides a safe detonation environment, isolated from other hosts on your network which you would not want to be infected. Additionally, most VM platforms such as VMWare Workstation provide tools for easily power cyclying and redeploying VMs.

Some malware developers do not wish to have their malware analyzed, or at least want to make the task more difficult. To this end, anti-sandbox measures are taken by developers, which will prevent the stage 1 malware from making calls to C&C servers to receive the stage 2 payload if the stage 1 detects it is running in a VM or other sandbox environment.

BearMetal aims to assist malware analysts by providing a baremetal platform with the aforementioned conveniences typically available only to virtual machines: easy disk redployment and remote control. To achieve this, iSCSI targets are created using layered instrumentation images and deployed to the baremetal systems remotely and IPMI is used for control. 

Put simply, a base OS image is created which has been specially configured to boot via the iSCSI protocol. On a separate image, software and other malware analysis tools are installed, called an "instrumented image". This secondary image is a differential between the base image and the changes made to the base. Finally on boot, a final instrumentation image is made between the secondary image and a final tertiary image. This ensures that any changes made by the malware are kept on reboot but can easily be wiped by deleting the tertiary image.
