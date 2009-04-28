%define name		pjl-comp-filter
%define version		1.7
%define release		1jpp
%define section		free

Name:           %{name}
Version:        %{version}
Release:        %{release}
Epoch:		0
Summary:    	A J2EE servlet filter which compresses data written to the response. 
License:        Apache License V2.0
Url:            http://sourceforge.net/projects/pjl-comp-filter/
Source0:        pjl-comp-filter-1.7.zip
Requires:	jpackage-utils >= 0:1.6
BuildRequires:  ant >= 0:1.6.2
BuildRequires:	junit >= 0:3.8.1
BuildRequires:	jpackage-utils >= 0:1.6
BuildRequires:	servletapi4 >= 0:4.0.4
Group:          Development/Java
Buildarch:      noarch
Buildroot:      %{_tmppath}/%{name}-%{version}-buildroot
Distribution:   JPackage
Vendor:         JPackage Project

%description
A J2EE servlet filter which compresses data written to the response. 
It supports several algorithms (gzip, deflate, etc.) and emphasizes 
minimal memory usage and high throughput. Also provides detailed 
performance stats.

%package javadoc
Summary:        Javadoc for %{name}
Group:          Development/Java

%description javadoc
Javadoc for %{name}.

%prep
%setup -q -c

%build
%ant -Dservlet.jar=$(build-classpath servletapi4) -Djunit.jar=$(build-classpath junit) release

%install
# jars
%__mkdir_p %{buildroot}%{_javadir}
%__install -m 644 %{name}-%{version}.jar %{buildroot}%{_javadir}/
(cd %{buildroot}%{_javadir} && for jar in *-%{version}*; do %__ln_s ${jar} ${jar/-%{version}/}; done)

# javadoc
%__mkdir_p %{buildroot}%{_javadocdir}/%{name}-%{version}
%__cp -a docs/* %{buildroot}%{_javadocdir}/%{name}-%{version}
(cd %{buildroot}%{_javadocdir} && %__ln_s %{name}-%{version} %{name})

%clean
%__rm -rf %{buildroot}

%post javadoc
%__rm -f %{_javadocdir}/%{name}
%__ln_s %{name}-%{version} %{_javadocdir}/%{name}

%postun javadoc
if [ $1 -eq 0 ]; then
  %__rm -f %{_javadocdir}/%{name}
fi

%files
%defattr(0644,root,root,0755)
%doc CHANGES.txt LICENSE.txt README.txt
%dir %{_javadir}
%{_javadir}/%{name}*.jar

%files javadoc
%defattr(0644,root,root,0755)
%dir %{_javadocdir}/%{name}-%{version}
%{_javadocdir}/%{name}-%{version}/*
%ghost %dir %{_javadocdir}/%{name}

%changelog
* Fri Mar 04 2005 Joe Wortmann <jwortmann@awarix.com> 0:1.4.1-1jpp
- Created



