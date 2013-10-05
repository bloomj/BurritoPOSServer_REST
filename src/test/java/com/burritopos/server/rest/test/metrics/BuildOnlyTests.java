package com.burritopos.server.rest.test.metrics;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import com.burritopos.server.rest.test.BuildTests;

@RunWith(Categories.class)
@IncludeCategory(BuildTests.class)
@SuiteClasses({ 
	AllTests.class })
    public class BuildOnlyTests {

    @BeforeClass 
    public static void setUpClass() {      

    }

    @AfterClass 
    public static void tearDownClass() { 

    }

}
