using System;
using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;
using Eto.Forms;
using Mendix.StudioPro.ExtensionsAPI.UI.Menu;
using Mendix.StudioPro.ExtensionsAPI.UI.Services;

namespace MyCompany.MyProject.MendixExtension
{
    [Export(typeof(MenuBarExtension))]
    class MyMenuExtension : MenuBarExtension
    {
        readonly IUserAuthenticationService authService;
        private readonly IDockingWindowService dockingWindowService;
        private static readonly HttpClient httpClient = new HttpClient();

        [ImportingConstructor]
        public MyMenuExtension(IUserAuthenticationService authService, IDockingWindowService dockingWindowService)
        {
            this.authService = authService;
            this.dockingWindowService = dockingWindowService;
        }

        public override IEnumerable<MenuViewModelBase> GetMenus()
        {
            yield return new MenuItemViewModel("My extension", placeUnder: new[] { "app" }, placeAfter: "tools")
            {
                Action = () => MessageBox.Show(authService.TryRetrieveUserName(out var userName) ? $"Hello {userName}!" : "Hello anonymous!")
            };

            yield return new MenuItemViewModel("Database Connection", placeUnder: new[] { "app" }, placeAfter: "tools")
            {
                Action = () => dockingWindowService.OpenPane(MyDockablePaneExtension.ID)
            };

            yield return new MenuItemViewModel("Start Jetty Server", placeUnder: new[] { "app" }, placeAfter: "tools")
            {
                Action = () => StartJettyServer()
            };
            yield return new MenuItemViewModel("Stop Jetty Server", placeUnder: new[] { "app" }, placeAfter: "tools")
            {
                Action = () => JettyServerRunner.StopJettyServer()
            };
            yield return new MenuItemViewModel("Vendorlib Path", placeUnder: new[] { "App" }, placeAfter: "Tools")
            {
                Action = async () =>
                {
                    try
                    {
                        string appRootDirectory = CurrentApp!.Root.DirectoryPath;
                        MessageBox.Show($"App Root Directory: {appRootDirectory}");

                        string vendorlibPath = Path.Combine(appRootDirectory, "vendorlib");

                        if (Directory.Exists(vendorlibPath))
                        {
                            MessageBox.Show($"Vendorlib Path: {vendorlibPath}");
                            await SendVendorlibPathToServer(vendorlibPath);
                        }
                        else
                        {
                            MessageBox.Show("Vendorlib directory not found.");
                        }
                    }
                    catch (Exception ex)
                    {
                        MessageBox.Show($"Failed to retrieve JAR file path: {ex.Message}");
                    }
                }
            };
        }

        private async Task SendVendorlibPathToServer(string vendorlibPath)
        {
            try
            {
                string escapedPath = vendorlibPath.Replace(@"\", @"\\");
                var content = new StringContent($"{{ \"path\": \"{escapedPath}\" }}", Encoding.UTF8, "application/json");
                var response = await httpClient.PostAsync("http://localhost:8081/setVendorlibPath", content);
                response.EnsureSuccessStatusCode();
                MessageBox.Show("Vendorlib Path Sent Successfully!");
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to send vendorlib path to server: {ex.Message}");
            }
        }


        private void StartJettyServer()
        {
            try
            {
                JettyServerRunner.StartJettyServer();
                MessageBox.Show("Jetty Server Started Successfully!");
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to start Jetty Server: {ex.Message}");
            }
        }
    }
}




