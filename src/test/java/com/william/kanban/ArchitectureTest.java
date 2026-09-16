package com.william.kanban;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures.LayeredArchitecture;
import com.william.kanban.entity.Account;
import com.william.kanban.service.AccountService;

@AnalyzeClasses(
	packages = "com.william.kanban",
	importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

	private static final String CONTROLLER = "com.william.kanban.controller..";

	private static final String SERVICE = "com.william.kanban.service..";

	private static final String MAPPER = "com.william.kanban.mapper..";

	private static final String REPOSITORY = "com.william.kanban.repository..";

	private static final String ENTITY = "com.william.kanban.entity..";

	private static final String DTO = "com.william.kanban.dto..";

	private static final String EXCEPTION = "com.william.kanban.exception..";

	@ArchTest
	static final ArchRule controllerOnlyAccessesServiceAndDto = layers()
		.whereLayer("Controller")
		.mayOnlyAccessLayers("Service", "Dto");

	@ArchTest
	static final ArchRule serviceOnlyAccessesItsLayers = layers()
		.whereLayer("Service")
		.mayOnlyAccessLayers("Repository", "Mapper", "Entity", "Dto", "Exception");

	@ArchTest
	static final ArchRule mapperOnlyAccessesEntityAndDto = layers()
		.whereLayer("Mapper")
		.mayOnlyAccessLayers("Entity", "Dto");

	@ArchTest
	static final ArchRule entityAndRepositoryStayBehindService = noClasses()
		.that()
		.resideOutsideOfPackages(SERVICE, REPOSITORY, MAPPER)
		.should()
		.dependOnClassesThat()
		.resideInAnyPackage(ENTITY, REPOSITORY);

	@ArchTest
	static final ArchRule onlyAccountServiceReadsPasswordHash = noClasses()
		.that()
		.doNotHaveFullyQualifiedName(AccountService.class.getName())
		.should()
		.callMethod(Account.class, "getPasswordHash");

	private static LayeredArchitecture layers() {
		return layeredArchitecture()
			.consideringOnlyDependenciesInLayers()
			.layer("Controller")
			.definedBy(CONTROLLER)
			.layer("Service")
			.definedBy(SERVICE)
			.layer("Mapper")
			.definedBy(MAPPER)
			.layer("Repository")
			.definedBy(REPOSITORY)
			.layer("Entity")
			.definedBy(ENTITY)
			.layer("Dto")
			.definedBy(DTO)
			.layer("Exception")
			.definedBy(EXCEPTION);
	}

}
