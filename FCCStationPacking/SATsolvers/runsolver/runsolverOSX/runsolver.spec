Name:		runsolver
Summary:	run a solver and tightly control the ressources it uses
Version:	3.3.0
Release:	1
License:	GPLv3
Group: 		Applications/System
Source0: 	%{name}-%{version}.tar.bz2
BuildRoot:	%{_tmppath}/%{name}-root
URL: 		http://www.cril.univ-artois.fr/~roussel/runsolver

%description

runsolver is a program meant to control the ressources used by a
solver. Basically, it can be seen as the integration of the time and
ulimit commands, but it has several other features. It is able to
timestamp each line output by a program, is able to handle correctly
multithreads or multiprocesses programs (don't loose the time of the
child even when the parent doesn't call wait()), is able to enforce
strict limits on the memory usage, logs information on each process
generated by the solver and is able to limit the size of the solver
output.

This program is especially useful when you want to run a program and
strictly control the ressources it uses.

This program was first used in the competitions of Pseudo-Boolean
solvers (2005) and was later used in the SAT and CSP competitions.

For now, the code is Linux specific.

%prep
%setup -q -n runsolver

%build
cd src
make

%install
cd src
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/usr/bin
make INSTROOT=$RPM_BUILD_ROOT install 

%clean
rm -rf $RPM_BUILD_ROOT

%files
%defattr(-,root,root)
/usr/bin/runsolver

%changelog
* Fri Nov 8 2009 roussel <roussel@cril.univ-artois.fr>
- Initial build.
